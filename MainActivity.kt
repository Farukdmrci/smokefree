package com.smokefree.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * ============================================================================
 * SMOKEFREE - Oyunlaştırılmış ve Ödül Odaklı Sigara Bırakma Uygulaması
 * ============================================================================
 * Kurulum Notları (build.gradle.kts - Module :app):
 * 1. Jetpack Compose ve Material3 bağımlılıklarını ekleyin:
 *    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
 *    implementation("androidx.compose.ui:ui")
 *    implementation("androidx.compose.material3:material3")
 *    implementation("androidx.compose.material:material-icons-extended")
 *    implementation("androidx.activity:activity-compose:1.9.0")
 *
 * 2. AndroidManifest.xml içerisine 4x2, 4x1, 2x2 ve 2x1 Widget tanımlamaları için
 *    SmokeFreeWidget4x2, SmokeFreeWidget4x1, SmokeFreeWidget2x2 ve SmokeFreeWidget2x1
 *    receiver'larını ekleyin.
 * ============================================================================
 */

// --- RENK PALETİ (Smoxy Esintili Modern Koyu & Zümrüt Yeşili Tema) ---
val BgDark = Color(0xFF0B1215)
val CardDark = Color(0xFF142127)
val CardBorder = Color(0xFF1F333D)
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldGlow = Color(0xFF059669)
val AmberGold = Color(0xFFF59E0B)
val CoralRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// --- VERİ MODELLERİ ---
data class UserConfig(
    val userName: String = "Faruk",
    val dailyCigarettes: Int = 20,
    val packPriceTl: Float = 75f,
    val packSize: Int = 20,
    val targetQuitDays: Int = 30,
    val startTimestampMillis: Long = System.currentTimeMillis(),
    val lastSmokeTimestampMillis: Long = System.currentTimeMillis(),
    val totalSmokedSinceStart: Int = 0,
    val points: Int = 120,
    val isSubscribed: Boolean = false,
    val onboardingCompleted: Boolean = false
)

data class CustomReward(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val costPoints: Int,
    val category: String,
    val isRedeemed: Boolean = false
)

data class HealthMilestone(
    val title: String,
    val durationHours: Double,
    val description: String,
    val letterTitle: String,
    val letterBody: String,
    val senderOrgan: String
)

// --- SAĞLIK ZAMAN ÇİZELGESİ VE VÜCUTTAN MEKTUPLAR ---
val HEALTH_MILESTONES = listOf(
    HealthMilestone(
        title = "20 Dakika: Kan Basıncı Dengelenir",
        durationHours = 0.33,
        description = "Kalp atış hızınız ve kan basıncınız normal değerlerine inmeye başlar.",
        letterTitle = "Kalbinden İlk Teşekkür Mektubu",
        letterBody = "Merhaba! Son sigarandan bu yana sadece 20 dakika geçti ama üzerindeki baskının azaldığını şimdiden hissedebiliyorum. Damarların gevşiyor, el ve ayakların normal sıcaklığına dönüyor. Lütfen böyle devam et!",
        senderOrgan = "Kalp ve Damar Sistemi"
    ),
    HealthMilestone(
        title = "8 Saat: Oksijen Seviyesi Artar",
        durationHours = 8.0,
        description = "Kandaki karbonmonoksit seviyesi yarıya iner, oksijen düzeyi normale döner.",
        letterTitle = "Kan Hücrelerinden Mesaj Var",
        letterBody = "Nihayet nefes alabiliyoruz! Saatlerdir taşıdığımız zehirli karbonmonoksit yarı yarıya azaldı. Artık dokularına ve beynine çok daha temiz oksijen taşıyoruz.",
        senderOrgan = "Dolaşım Sistemi"
    ),
    HealthMilestone(
        title = "24 Saat: Kalp Krizi Riski Düşer",
        durationHours = 24.0,
        description = "Vücudunuz karbonmonoksitten tamamen arınır, kalp krizi riskiniz azalmaya başlar.",
        letterTitle = "İlk Tam Günümüz Kutlu Olsun!",
        letterBody = "Tam 24 saattir duman yok! Akciğerlerin biriken mukusu ve toksinleri temizlemek için büyük bir bahar temizliği başlattı. Bugün kendin için harika bir adım attın.",
        senderOrgan = "Akciğerler ve Kalp"
    ),
    HealthMilestone(
        title = "48 Saat: Tat ve Koku Duyuları Canlanır",
        durationHours = 48.0,
        description = "Sinir uçları yenilenmeye başlar; yemeklerin tadını ve kokuları çok daha net alırsınız.",
        letterTitle = "Dünyanın Kokusu Değişti",
        letterBody = "Nikotin vücudundan tamamen çıktı. Hasar gören sinir uçlarımız yeniden filizleniyor. Sabah kahvenin kokusunu ve yemeklerin gerçek lezzetini yeniden keşfetmeye hazır ol!",
        senderOrgan = "Sinir ve Duyu Sistemi"
    ),
    HealthMilestone(
        title = "72 Saat: Bronşlar Rahatlar",
        durationHours = 72.0,
        description = "Bronş tüpleri gevşer, nefes alıp vermek belirgin şekilde kolaylaşır ve enerjiniz artar.",
        letterTitle = "Derin Bir Nefes Al!",
        letterBody = "Üçüncü günü geride bıraktık! Hava yollarımız olan bronşlar artık kasılmıyor. Merdiven çıkarken veya yürürken nefesinin ne kadar açıldığını fark edeceksin.",
        senderOrgan = "Solunum Yolları"
    ),
    HealthMilestone(
        title = "2 Hafta: Akciğer Kapasitesi %30 Artar",
        durationHours = 336.0,
        description = "Kan dolaşımı büyük ölçüde iyileşir, akciğer fonksiyonları %30'a varan oranda yükselir.",
        letterTitle = "Yeniden Doğuş Mektubu",
        letterBody = "İki haftalık kararlılığın sayesinde kapasitemiz %30 arttı. Koşabilir, derin nefes alabilir ve sabahları çok daha dinç uyanabilirsin. Bize ikinci bir hayat verdin!",
        senderOrgan = "Akciğerler"
    )
)

// --- YEREL DEPO YÖNETİCİSİ (SharedPreferences) ---
class SmokeFreePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smokefree_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): UserConfig {
        return UserConfig(
            userName = prefs.getString("user_name", "Faruk") ?: "Faruk",
            dailyCigarettes = prefs.getInt("daily_cigs", 20),
            packPriceTl = prefs.getFloat("pack_price", 75f),
            packSize = prefs.getInt("pack_size", 20),
            targetQuitDays = prefs.getInt("target_days", 30),
            startTimestampMillis = prefs.getLong("start_ts", System.currentTimeMillis()),
            lastSmokeTimestampMillis = prefs.getLong("last_smoke_ts", System.currentTimeMillis()),
            totalSmokedSinceStart = prefs.getInt("total_smoked", 0),
            points = prefs.getInt("points", 120),
            isSubscribed = prefs.getBoolean("is_subscribed", false),
            onboardingCompleted = prefs.getBoolean("onboarding_done", false)
        )
    }

    fun saveConfig(config: UserConfig) {
        prefs.edit()
            .putString("user_name", config.userName)
            .putInt("daily_cigs", config.dailyCigarettes)
            .putFloat("pack_price", config.packPriceTl)
            .putInt("pack_size", config.packSize)
            .putInt("target_days", config.targetQuitDays)
            .putLong("start_ts", config.startTimestampMillis)
            .putLong("last_smoke_ts", config.lastSmokeTimestampMillis)
            .putInt("total_smoked", config.totalSmokedSinceStart)
            .putInt("points", config.points)
            .putBoolean("is_subscribed", config.isSubscribed)
            .putBoolean("onboarding_done", config.onboardingCompleted)
            .apply()
    }

    fun loadRewards(): List<CustomReward> {
        val raw = prefs.getString("custom_rewards", null)
        if (raw.isNullOrBlank()) {
            return listOf(
                CustomReward("1", "Favori Kahveciden Özel Kahve", 150, "Keyif", false),
                CustomReward("2", "Sinema Bileti & Mısır Menüsü", 400, "Eğlence", false),
                CustomReward("3", "Yeni Kablosuz Kulaklık Fonu", 1500, "Teknoloji", false),
                CustomReward("4", "Hafta Sonu Doğa Kaçamağı", 3000, "Deneyim", false)
            )
        }
        return raw.split(";;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 5) {
                CustomReward(
                    id = parts[0],
                    title = parts[1],
                    costPoints = parts[2].toIntOrNull() ?: 100,
                    category = parts[3],
                    isRedeemed = parts[4].toBoolean()
                )
            } else null
        }
    }

    fun saveRewards(rewards: List<CustomReward>) {
        val serialized = rewards.joinToString(";;") {
            "${it.id}|${it.title}|${it.costPoints}|${it.category}|${it.isRedeemed}"
        }
        prefs.edit().putString("custom_rewards", serialized).apply()
    }
}

// --- AŞAMALI SİGARA ARALIĞI VE İSTATİSTİK HESAPLAYICI ---
object SmokeFreeCalculator {
    /**
     * Kullanıcının başlangıçtaki günlük sigara sayısına ve hedef bırakma gününe göre
     * iki sigara arasında beklemesi gereken güncel süreyi (dakika cinsinden) hesaplar.
     * Gün geçtikçe bekleme süresi kademeli olarak uzar.
     */
    fun currentRequiredIntervalMinutes(config: UserConfig, nowMillis: Long): Int {
        val awakeMinutesPerDay = 16 * 60 // 960 aktif dakika
        val baseInterval = max(15, awakeMinutesPerDay / max(1, config.dailyCigarettes))
        val elapsedDays = max(
            0.0,
            (nowMillis - config.startTimestampMillis).toDouble() / TimeUnit.DAYS.toMillis(1)
        )
        val progressRatio = min(1.0, elapsedDays / max(1, config.targetQuitDays))
        // Hedef güne yaklaştıkça aralık kademeli olarak 4 katına kadar uzatılır
        val multiplier = 1.0 + (progressRatio * 3.0)
        return (baseInterval * multiplier).roundToInt()
    }

    fun savedCigarettes(config: UserConfig, nowMillis: Long): Int {
        val elapsedHours = max(
            0.0,
            (nowMillis - config.startTimestampMillis).toDouble() / TimeUnit.HOURS.toMillis(1)
        )
        val expectedSmoked = ((config.dailyCigarettes / 24.0) * elapsedHours).roundToInt()
        return max(0, expectedSmoked - config.totalSmokedSinceStart)
    }

    fun savedMoneyTl(config: UserConfig, nowMillis: Long): Float {
        val pricePerCig = config.packPriceTl / max(1, config.packSize)
        return savedCigarettes(config, nowMillis) * pricePerCig
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = max(0L, millis / 1000L)
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (days > 0) {
            String.format("%dG %02dS %02dD", days, hours, minutes)
        } else {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}

// --- ANA AKTİVİTE ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = EmeraldPrimary,
                    secondary = AmberGold,
                    background = BgDark,
                    surface = CardDark,
                    onPrimary = Color.Black,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    SmokeFreeAppRoot()
                }
            }
        }
    }
}

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Ana Ekran", Icons.Default.Timer),
    REWARDS("Ödüller", Icons.Default.CardGiftcard),
    HEALTH("Sağlık & Mektup", Icons.Default.Favorite),
    STATS("Takvim & Analiz", Icons.Default.BarChart),
    PROFILE("Premium & Profil", Icons.Default.WorkspacePremium)
}

@Composable
fun SmokeFreeAppRoot() {
    val context = LocalContext.current
    val repo = remember { SmokeFreePreferences(context) }
    var config by remember { mutableStateOf(repo.loadConfig()) }
    var rewards by remember { mutableStateOf(repo.loadRewards()) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Canlı sayaç için her saniye güncelleme
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    fun updateConfig(newConfig: UserConfig) {
        config = newConfig
        repo.saveConfig(newConfig)
        SmokeFreeWidgetUpdater.updateAllWidgets(context)
    }

    fun updateRewards(newRewards: List<CustomReward>) {
        rewards = newRewards
        repo.saveRewards(newRewards)
    }

    if (!config.onboardingCompleted) {
        OnboardingScreen(
            initialConfig = config,
            onComplete = { updated ->
                updateConfig(updated.copy(onboardingCompleted = true))
            }
        )
    } else {
        Scaffold(
            containerColor = BgDark,
            bottomBar = {
                NavigationBar(
                    containerColor = CardDark,
                    tonalElevation = 8.dp
                ) {
                    BottomTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    BottomTab.HOME -> DashboardScreen(
                        config = config,
                        nowMillis = nowMillis,
                        onDelayCigarette = {
                            // Sigarayı erteleyince +20 puan ver
                            updateConfig(config.copy(points = config.points + 20))
                        },
                        onSmokeNow = {
                            val reqIntervalMs = SmokeFreeCalculator
                                .currentRequiredIntervalMinutes(config, nowMillis) * 60_000L
                            val waitedFullInterval =
                                (nowMillis - config.lastSmokeTimestampMillis) >= reqIntervalMs
                            val earnedPoints = if (waitedFullInterval) 15 else 0
                            updateConfig(
                                config.copy(
                                    lastSmokeTimestampMillis = nowMillis,
                                    totalSmokedSinceStart = config.totalSmokedSinceStart + 1,
                                    points = config.points + earnedPoints
                                )
                            )
                        }
                    )
                    BottomTab.REWARDS -> RewardStoreScreen(
                        config = config,
                        rewards = rewards,
                        onAddReward = { title, cost, category ->
                            val updated = rewards + CustomReward(
                                title = title,
                                costPoints = cost,
                                category = category
                            )
                            updateRewards(updated)
                        },
                        onRedeemReward = { reward ->
                            if (config.points >= reward.costPoints && !reward.isRedeemed) {
                                updateConfig(config.copy(points = config.points - reward.costPoints))
                                updateRewards(
                                    rewards.map {
                                        if (it.id == reward.id) it.copy(isRedeemed = true) else it
                                    }
                                )
                            }
                        }
                    )
                    BottomTab.HEALTH -> HealthTimelineScreen(
                        config = config,
                        nowMillis = nowMillis
                    )
                    BottomTab.STATS -> StatsAndWidgetsPreviewScreen(
                        config = config,
                        nowMillis = nowMillis
                    )
                    BottomTab.PROFILE -> SubscriptionAndProfileScreen(
                        config = config,
                        onToggleSubscription = {
                            updateConfig(config.copy(isSubscribed = !config.isSubscribed))
                        },
                        onResetOnboarding = {
                            updateConfig(config.copy(onboardingCompleted = false))
                        }
                    )
                }
            }
        }
    }
}

// --- 1. KARŞILAMA VE HEDEF BELİRLEME (ONBOARDING) EKRANI ---
@Composable
fun OnboardingScreen(
    initialConfig: UserConfig,
    onComplete: (UserConfig) -> Unit
) {
    var name by remember { mutableStateOf(initialConfig.userName) }
    var dailyCigs by remember { mutableStateOf(initialConfig.dailyCigarettes.toString()) }
    var packPrice by remember { mutableStateOf(initialConfig.packPriceTl.toInt().toString()) }
    var selectedTargetDays by remember { mutableStateOf(initialConfig.targetQuitDays) }

    val targetOptions = listOf(14, 30, 45, 60, 90)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                }
                Column {
                    Text(
                        text = "SMOKEFREE",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "Aşamalı ve Ödül Odaklı Sigara Bırakma Koçun",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "Kişisel Bırakma Planını Oluşturalım",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Adınız") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dailyCigs,
                    onValueChange = { dailyCigs = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Günlük Sigara Adedi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = packPrice,
                    onValueChange = { packPrice = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Paket Fiyatı (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Ne Zaman Tamamen Bırakmak İstiyorsun?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(targetOptions) { days ->
                    val isSelected = selectedTargetDays == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) EmeraldPrimary else CardDark
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldLight else CardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedTargetDays = days }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$days Gün",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                            Text(
                                text = "Aşamalı Plan",
                                fontSize = 11.sp,
                                color = if (isSelected) Color.Black.copy(alpha = 0.75f) else TextSecondary
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nasıl Çalışır?",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Sigara içme aralıkların her gün kademeli olarak uzatılır.\n" +
                            "• Süre dolmadan sigara içmezsen ödül puanları kazanırsın.\n" +
                            "• Kazandığın puanlarla kendi belirlediğin ödülleri açarsın.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Button(
            onClick = {
                val now = System.currentTimeMillis()
                onComplete(
                    initialConfig.copy(
                        userName = name.ifBlank { "Faruk" },
                        dailyCigarettes = dailyCigs.toIntOrNull()?.coerceIn(1, 100) ?: 20,
                        packPriceTl = packPrice.toFloatOrNull()?.coerceAtLeast(10f) ?: 75f,
                        targetQuitDays = selectedTargetDays,
                        startTimestampMillis = now,
                        lastSmokeTimestampMillis = now
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "SMOKEFREE Planımı Başlat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

// --- 2. ANA EKRAN (DASHBOARD - SAYAÇ VE AŞAMALI GECİKTİRME) ---
@Composable
fun DashboardScreen(
    config: UserConfig,
    nowMillis: Long,
    onDelayCigarette: () -> Unit,
    onSmokeNow: () -> Unit
) {
    val smokeFreeDurationMs = max(0L, nowMillis - config.lastSmokeTimestampMillis)
    val requiredIntervalMin = SmokeFreeCalculator.currentRequiredIntervalMinutes(config, nowMillis)
    val requiredIntervalMs = requiredIntervalMin * 60_000L
    val remainingWaitMs = max(0L, requiredIntervalMs - smokeFreeDurationMs)
    val intervalProgress = min(1f, smokeFreeDurationMs.toFloat() / max(1L, requiredIntervalMs))
    val animatedProgress by animateFloatAsState(
        targetValue = intervalProgress,
        animationSpec = tween(durationMillis = 500),
        label = "interval_ring"
    )

    val savedCigs = SmokeFreeCalculator.savedCigarettes(config, nowMillis)
    val savedTl = SmokeFreeCalculator.savedMoneyTl(config, nowMillis)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Merhaba, ${config.userName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Hedef: ${config.targetQuitDays} Günde Tamamen Bırakma",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = AmberGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${config.points} Puan",
                            fontWeight = FontWeight.Bold,
                            color = AmberGold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Aşamalı Bekleme Çemberi ve Canlı Sayaç
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SİGARASIZ GEÇEN SÜRE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(210.dp)
                    ) {
                        Canvas(modifier = Modifier.size(190.dp)) {
                            drawArc(
                                color = CardBorder,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(EmeraldPrimary, EmeraldLight, EmeraldPrimary)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SmokeFreeCalculator.formatDuration(smokeFreeDurationMs),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (remainingWaitMs > 0) {
                                Text(
                                    text = "Sonraki mola: ${SmokeFreeCalculator.formatDuration(remainingWaitMs)}",
                                    fontSize = 12.sp,
                                    color = AmberGold
                                )
                            } else {
                                Text(
                                    text = "Bekleme hedefini tamamladın!",
                                    fontSize = 12.sp,
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Güncel aralık: $requiredIntervalMin dk",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDelayCigarette,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Ertele (+20 Puan)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onSmokeNow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Sigara İçtim",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Özet Metrik Kartları (Tasarruf ve İçilmeyen Sigara)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Para Tasarrufu",
                    value = String.format("₺%.1f", savedTl),
                    subtitle = "Cebinde kalan tutar",
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "İçilmeyen Sigara",
                    value = "$savedCigs Adet",
                    subtitle = "Kazanılan temiz nefes",
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Günlük Görev Kartı
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Günün Oyunlaştırılmış Görevleri",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    TaskRow("İlk sigaranı uyandıktan 45 dk sonraya ertele", "+25 Puan", true)
                    TaskRow("Belirlenen $requiredIntervalMin dakikalık aralığa uy", "+15 Puan", remainingWaitMs == 0L)
                    TaskRow("Kendi ödül mağazana yeni bir hedef ekle", "+10 Puan", true)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun TaskRow(title: String, rewardBadge: String, completed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (completed) EmeraldPrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                color = if (completed) TextPrimary else TextSecondary
            )
        }
        Text(
            text = rewardBadge,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AmberGold
        )
    }
}

// --- 3. KİŞİSEL ÖDÜL MAĞAZASI EKRANI ---
@Composable
fun RewardStoreScreen(
    config: UserConfig,
    rewards: List<CustomReward>,
    onAddReward: (String, Int, String) -> Unit,
    onRedeemReward: (CustomReward) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newCost by remember { mutableStateOf("250") }
    var newCategory by remember { mutableStateOf("Kişisel Hedef") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ödül Mağazası",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Kendi ödüllerini ekle, sigarayı erteledikçe kazan!",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ödül Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kullanılabilir Ödül Bakiyen:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${config.points} Puan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AmberGold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rewards, key = { it.id }) { reward ->
                val progress = min(1f, config.points.toFloat() / max(1, reward.costPoints))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reward.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                                Text(
                                    text = reward.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Button(
                                onClick = { onRedeemReward(reward) },
                                enabled = !reward.isRedeemed && config.points >= reward.costPoints,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (reward.isRedeemed) CardBorder else EmeraldPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (reward.isRedeemed) "Alındı ✓" else "${reward.costPoints} Puan",
                                    color = if (reward.isRedeemed) TextSecondary else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { if (reward.isRedeemed) 1f else progress },
                            color = EmeraldPrimary,
                            trackColor = CardBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardDark,
            title = {
                Text("Yeni Kişisel Ödül Ekle", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Ödül Adı (Örn: Yeni Spor Ayakkabı)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCost,
                        onValueChange = { newCost = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Hedef Puan Değeri") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Kategori") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddReward(
                                newTitle.trim(),
                                newCost.toIntOrNull()?.coerceAtLeast(10) ?: 200,
                                newCategory.ifBlank { "Kişisel" }
                            )
                            newTitle = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal", color = TextSecondary)
                }
            }
        )
    }
}

// --- 4. VÜCUT İYİLEŞME ZAMAN ÇİZELGESİ VE MEKTUPLAR EKRANI ---
@Composable
fun HealthTimelineScreen(
    config: UserConfig,
    nowMillis: Long
) {
    val smokeFreeHours = max(
        0.0,
        (nowMillis - config.lastSmokeTimestampMillis).toDouble() / TimeUnit.HOURS.toMillis(1)
    )
    var selectedLetter by remember { mutableStateOf<HealthMilestone?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(
                text = "Vücut İyileşme Zaman Çizelgesi",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Sağlık aşamalarını tamamladıkça vücudundan gelen mektupların kilidi açılır.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        items(HEALTH_MILESTONES) { milestone ->
            val progress = min(1.0, smokeFreeHours / milestone.durationHours).toFloat()
            val isUnlocked = progress >= 1f

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isUnlocked) EmeraldPrimary else CardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = milestone.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isUnlocked) EmeraldLight else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%${(progress * 100).roundToInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isUnlocked) EmeraldPrimary else AmberGold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = milestone.description,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (isUnlocked) EmeraldPrimary else AmberGold,
                        trackColor = CardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { selectedLetter = milestone },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.MarkEmailRead else Icons.Default.MailOutline,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mektubu Oku: ${milestone.letterTitle}",
                            color = EmeraldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    selectedLetter?.let { letter ->
        AlertDialog(
            onDismissRequest = { selectedLetter = null },
            containerColor = CardDark,
            title = {
                Column {
                    Text(
                        text = letter.senderOrgan.uppercase(),
                        fontSize = 11.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = letter.letterTitle,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = letter.letterBody,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedLetter = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Teşekkürler", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- 5. İSTATİSTİKLER VE ANDROID WIDGET ÖNİZLEME EKRANI (4x2, 4x1, 2x2, 2x1) ---
@Composable
fun StatsAndWidgetsPreviewScreen(
    config: UserConfig,
    nowMillis: Long
) {
    val smokeFreeText = SmokeFreeCalculator.formatDuration(nowMillis - config.lastSmokeTimestampMillis)
    val savedTl = SmokeFreeCalculator.savedMoneyTl(config, nowMillis)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(
                text = "Bırakma Takvimi & Android Widget'ları",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Ana ekrana ekleyebileceğin 4x2, 4x1, 2x2 ve 2x1 SMOKEFREE widget görünümleri:",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // 4x2 Widget Önizlemesi
        item {
            Text("4x2 Widget (Büyük Sayaç, Puan ve Tasarruf)", fontSize = 13.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SMOKEFREE • 4x2", fontSize = 10.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(smokeFreeText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Sigarasız Geçen Süre", fontSize = 11.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format("₺%.1f Tasarruf", savedTl), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${config.points} Ödül Puanı", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }
                }
            }
        }

        // 4x1 Widget Önizlemesi
        item {
            Text("4x1 Widget (Süre ve Hedef Çubuğu)", fontSize = 13.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sigarasız: $smokeFreeText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Hedef: ${config.targetQuitDays} Gün", fontSize = 12.sp, color = EmeraldLight)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.65f },
                        color = EmeraldPrimary,
                        trackColor = CardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // 2x2 ve 2x1 Widget Önizlemeleri Yan Yana
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("2x2 Widget", fontSize = 13.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SMOKEFREE", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            Text(smokeFreeText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Her nefes bir zafer!", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("2x1 Widget", fontSize = 13.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = EmeraldPrimary)
                            Text(smokeFreeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// --- 6. ABONELİK (45 TL / AY) VE PROFİL EKRANI ---
@Composable
fun SubscriptionAndProfileScreen(
    config: UserConfig,
    onToggleSubscription: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SMOKEFREE Premium & Profil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, EmeraldPrimary, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMOKEFREE KOÇ ABONELİĞİ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldLight
                    )
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (config.isSubscribed) "AKTİF" else "45 TL / Ay",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aylık 45 TL — Yarım Paket Sigara Fiyatına Tam Özgürlük",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Sınırsız özel ödül ekleme ve oyunlaştırılmış görevler\n" +
                        "• Tüm 4x2, 4x1, 2x2 ve 2x1 Android ana ekran widget'ları\n" +
                        "• Vücut iyileşme mektupları ve akıllı mola bildirimleri",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onToggleSubscription,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (config.isSubscribed) CardBorder else EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (config.isSubscribed) "Aboneliği Yönet (Aktif)" else "Aylık 45 TL ile Premium'a Geç",
                        color = if (config.isSubscribed) TextPrimary else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Plan Ayarların", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Kullanıcı: ${config.userName}", fontSize = 13.sp, color = TextSecondary)
                Text("Başlangıç Günlük Sigara: ${config.dailyCigarettes} adet", fontSize = 13.sp, color = TextSecondary)
                Text("Paket Fiyatı: ₺${config.packPriceTl.toInt()}", fontSize = 13.sp, color = TextSecondary)
                Text("Bırakma Hedefi: ${config.targetQuitDays} Gün", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onResetOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hedef ve Sigara Bilgilerimi Güncelle", color = EmeraldLight)
                }
            }
        }
    }
}

// --- 7. ANDROID ANA EKRAN WIDGET SAĞLAYICILARI (4x2, 4x1, 2x2, 2x1) ---
object SmokeFreeWidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            SmokeFreeWidget4x2::class.java,
            SmokeFreeWidget4x1::class.java,
            SmokeFreeWidget2x2::class.java,
            SmokeFreeWidget2x1::class.java
        )
        providers.forEach { providerClass ->
            val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    fun buildRemoteViews(context: Context, title: String, subtitle: String): RemoteViews {
        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        views.setTextViewText(android.R.id.text1, title)
        views.setTextViewText(android.R.id.text2, subtitle)
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(android.R.id.text1, pendingIntent)
        return views
    }
}

open class BaseSmokeFreeWidget(private val sizeTag: String) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val config = SmokeFreePreferences(context).loadConfig()
        val now = System.currentTimeMillis()
        val duration = SmokeFreeCalculator.formatDuration(now - config.lastSmokeTimestampMillis)
        val savedTl = SmokeFreeCalculator.savedMoneyTl(config, now)

        val title = "SMOKEFREE ($sizeTag) • $duration"
        val subtitle = String.format("Tasarruf: ₺%.1f | Puan: %d", savedTl, config.points)

        for (widgetId in appWidgetIds) {
            val views = SmokeFreeWidgetUpdater.buildRemoteViews(context, title, subtitle)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

class SmokeFreeWidget4x2 : BaseSmokeFreeWidget("4x2")
class SmokeFreeWidget4x1 : BaseSmokeFreeWidget("4x1")
class SmokeFreeWidget2x2 : BaseSmokeFreeWidget("2x2")
class SmokeFreeWidget2x1 : BaseSmokeFreeWidget("2x1")
