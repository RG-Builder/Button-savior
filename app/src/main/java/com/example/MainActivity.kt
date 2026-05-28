package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SettingsEntity
import com.example.data.StatsEntity
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val isServiceActiveState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val stats by viewModel.statsState.collectAsStateWithLifecycle()
            val isServiceActive by isServiceActiveState

            CustomThemeWrapper(themeName = settings.themeName) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = getThemeBackgroundColor(settings.themeName)
                ) { innerPadding ->
                    MainScreen(
                        settings = settings,
                        stats = stats,
                        isServiceActive = isServiceActive,
                        onOpenSettingsClick = { openAccessibilitySettings(this) },
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isServiceActiveState.value = isAccessibilityServiceEnabled(this)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, AssistiveTouchService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun CustomThemeWrapper(themeName: String, content: @Composable () -> Unit) {
    MyApplicationTheme(
        darkTheme = true, // Force amoled-supporting dark palettes across all selections
        dynamicColor = false,
        content = content
    )
}

// Custom AMOLED optimized background palettes to eliminate battery drain and maximize eye-safety
fun getThemeBackgroundColor(theme: String): Color {
    return when (theme) {
        "MINIMAL" -> Color(0xFF0C0C0E) // Obsidian Slate
        "ELITE" -> Color(0xFF000000)   // Zero Matrix Space Black
        else -> Color(0xFF0B0813)      // Velvet Midnight Orchid
    }
}

fun getThemeCardBackgroundColor(theme: String): Color {
    return when (theme) {
        "MINIMAL" -> Color(0xFF16161A) // Elegant Dark Slate Card
        "ELITE" -> Color(0xFF060A07)   // Terminal green matrix card
        else -> Color(0xFF151025)      // Luxurious Translucent Space Orchid Card
    }
}

fun getThemeAccentColor(theme: String): Color {
    return when (theme) {
        "MINIMAL" -> Color(0xFFABC8F7) // Silent Soft Blue
        "ELITE" -> Color(0xFF00FF66)   // Cyber matrix green
        else -> Color(0xFFD495FF)      // Amethyst lilac orchid
    }
}

fun getThemeTextColor(theme: String): Color {
    return when (theme) {
        "ELITE" -> Color(0xFF00FF66)
        "MINIMAL" -> Color(0xFFE2E2E2)
        else -> Color(0xFFF5E1FF)
    }
}

fun getThemeSubtextColor(theme: String): Color {
    return when (theme) {
        "ELITE" -> Color(0xFF05AA3D)
        "MINIMAL" -> Color(0xFF8D9199)
        else -> Color(0xFFB1A9CD)
    }
}

@Composable
fun MainScreen(
    settings: SettingsEntity,
    stats: List<StatsEntity>,
    isServiceActive: Boolean,
    onOpenSettingsClick: () -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Customization", "Impact Stats")
    val theme = settings.themeName

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App banner with dynamic theme accent
        AppHeaderBanner(theme)

        // Navigation Tabs Row
        TabSelector(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            theme = theme
        )

        Divider(
            color = getThemeAccentColor(theme).copy(alpha = 0.15f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Switcher with smooth transitional fade
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "TabTransition"
        ) { targetTab ->
            when (targetTab) {
                0 -> DashboardPane(
                    isServiceActive = isServiceActive,
                    onOpenSettingsClick = onOpenSettingsClick,
                    settings = settings,
                    viewModel = viewModel,
                    theme = theme
                )
                1 -> CustomizationPane(
                    settings = settings,
                    viewModel = viewModel,
                    theme = theme
                )
                2 -> StatsPane(
                    stats = stats,
                    viewModel = viewModel,
                    theme = theme
                )
            }
        }
    }
}

@Composable
fun AppHeaderBanner(theme: String) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(
                BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
                RoundedCornerShape(28.dp)
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (theme) {
                    "MINIMAL" -> Icons.Default.Circle
                    "ELITE" -> Icons.Default.Code
                    else -> Icons.Default.TouchApp
                },
                contentDescription = "App Icon",
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Silent Motion",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = text,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )
            Text(
                text = "Adaptive alternative touch navigator",
                fontSize = 12.sp,
                color = subtext,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}

@Composable
fun TabSelector(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    theme: String
) {
    val accent = getThemeAccentColor(theme)
    val containerBg = getThemeCardBackgroundColor(theme).copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(containerBg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.15f)), RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (isSelected) accent else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (theme == "ELITE") Color.Black else getThemeBackgroundColor(theme)
                    } else {
                        getThemeSubtextColor(theme)
                    },
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )
            }
        }
    }
}

@Composable
fun DashboardPane(
    isServiceActive: Boolean,
    onOpenSettingsClick: () -> Unit,
    settings: SettingsEntity,
    viewModel: MainViewModel,
    theme: String
) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Condition 1: INACTIVE Service - Launch Obstacle, Guide through the "Onboarding/Getting Setup Wizard"
        if (!isServiceActive) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.5.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "GETTING SETUP wizard",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }

                Text(
                    "Authorize accessibility overlay and displays so the visual gesture trigger panels can draw seamlessly over native views.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = text,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                // Mock Stepper Track Progress
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(accent))
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)))
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)))
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Active Steps Box
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Step 1: Active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = "Step 1",
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Step 1: Allow Gesture Controls",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = text,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                            Text(
                                "Grant secondary touch overlay control permissions",
                                fontSize = 10.sp,
                                color = subtext,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }

                    // Pre-locked Steps 2, 3, 4 Faded Mock Indicators
                    listOf(
                        "Step 2: Display Over Other Apps" to Icons.Default.Layers,
                        "Step 3: Battery Optimization Safe" to Icons.Default.BatteryChargingFull,
                        "Step 4: Configure Exclusion Apps" to Icons.Default.AppBlocking
                    ).forEach { stepPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.4f)
                                .clip(RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = stepPair.second,
                                contentDescription = "Locked",
                                tint = subtext,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    stepPair.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = text,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                                Text(
                                    "Pending Step 1 completion",
                                    fontSize = 9.sp,
                                    color = subtext,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Lock, "Lock", tint = subtext, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (theme == "ELITE") Color.Black else getThemeBackgroundColor(theme)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Settings, "Enable")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Grant Service Access",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        } else {
            // Condition 2: ACTIVE Status Card with Radiant Glow
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.4f)), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pulse = rememberInfiniteTransition(label = "pulse")
                        val alphaState by pulse.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1100, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulser"
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(alphaState)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TOUCH HELPER ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }

                    Badge(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)) {
                        Text(
                            "RUNNING",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                }

                Text(
                    "Your virtual savior touch-trigger is live on screen. Tap or swipe edges dynamically to simulate critical navigation tasks instantly.",
                    fontSize = 13.sp,
                    color = subtext,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                // 3-Column Indicator Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Engine" to "Active",
                        "Accessibility" to "Granted",
                        "Calibration" to "Optimal"
                    ).forEach { indicator ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(accent.copy(alpha = 0.05f))
                                .border(BorderStroke(1.dp, accent.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                indicator.first,
                                fontSize = 9.sp,
                                color = subtext,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                indicator.second,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.SettingsAccessibility, "Accessibility")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Manage Accessibility Support",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }

        // Live Preview Frame Card
        VisualLivePreviewSection(theme = theme, settings = settings)

        // Sandbox simulated interactive tester
        InteractiveSandboxPanel(theme = theme, viewModel = viewModel, shortcuts = settings.enabledShortcuts)
    }
}

@Composable
fun VisualLivePreviewSection(theme: String, settings: SettingsEntity) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.15f)), RoundedCornerShape(28.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Floating Trigger Preview",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = text,
            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
        )

        Spacer(modifier = Modifier.height(6.dp))

        val sizeDp = settings.buttonSizeDp.dp
        Box(
            modifier = Modifier
                .size(sizeDp)
                .alpha(settings.buttonOpacity)
                .clip(CircleShape)
                .background(
                    when (theme) {
                        "MINIMAL" -> Brush.verticalGradient(listOf(Color(0xFF2E2E2E), Color(0xFF141414)))
                        "ELITE" -> Brush.verticalGradient(listOf(Color(0xFF0B140E), Color(0xFF020503)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                    }
                )
                .border(
                    BorderStroke(2.dp, if (theme == "ELITE") Color(0xFF00FF66) else Color.White),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (theme) {
                "MINIMAL" -> Box(modifier = Modifier.size(sizeDp / 3).clip(CircleShape).background(Color.White))
                "ELITE" -> Icon(Icons.Default.Code, "Menu", tint = Color(0xFF00FF66), modifier = Modifier.size(sizeDp / 2))
                else -> Icon(Icons.Default.TouchApp, "Menu", tint = Color.White, modifier = Modifier.size(sizeDp * 0.55f))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Selected Theme: $theme (Size: ${settings.buttonSizeDp}dp, Opacity: ${(settings.buttonOpacity * 100).toInt()}%)",
            fontSize = 11.sp,
            color = subtext,
            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun InteractiveSandboxPanel(theme: String, viewModel: MainViewModel, shortcuts: String) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)
    val shortcutList = shortcuts.split(",")

    var lastSimulatedAction by remember { mutableStateOf<String?>(null) }
    var interactionCount by remember { mutableIntStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Interactive Sandbox (Tester)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )

            Text(
                text = "Tap simulated coordinates to calibrate overlay trigger paths and verify click logs without leaving settings.",
                fontSize = 11.sp,
                color = subtext,
                textAlign = TextAlign.Center,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )

            val testButtons = listOf(
                Triple("BACK", "Back", Icons.AutoMirrored.Filled.ArrowBack),
                Triple("HOME", "Home", Icons.Default.Home),
                Triple("RECENTS", "Recents", Icons.Default.Menu),
                Triple("NOTIFICATIONS", "Notification", Icons.Default.Notifications),
                Triple("LOCK_SCREEN", "Lock Screen", Icons.Default.Lock),
                Triple("VOLUME_UP", "Raise Vol", Icons.Default.VolumeUp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                testButtons.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (btn in row) {
                            val isConfigured = shortcutList.contains(btn.first)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isConfigured) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isConfigured) accent.copy(alpha = 0.4f) else accent.copy(alpha = 0.1f)
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        viewModel.simulateActionClick(btn.first)
                                        lastSimulatedAction = btn.second
                                        interactionCount++
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = btn.third,
                                    contentDescription = btn.second,
                                    tint = if (isConfigured) accent else subtext,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    btn.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = text,
                                    textAlign = TextAlign.Center,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = lastSimulatedAction != null) {
                lastSimulatedAction?.let { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.1f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Check, "Checked", tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Simulated touch registered: $action (+1 Save Logged)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationPane(
    settings: SettingsEntity,
    viewModel: MainViewModel,
    theme: String
) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // High-Fidelity Phone Screen Visual Mock Preview (from Mockup Customization guidelines)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.15f)), RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Dynamic Edge Zone Preview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = text,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                // Render Smartphone Device Outline Box
                Box(
                    modifier = Modifier
                        .width(190.dp)
                        .height(340.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF000000))
                        .border(BorderStroke(6.dp, Color(0xFF2E2E33)), RoundedCornerShape(32.dp))
                ) {
                    // Mobile Abstract wallpaper gradient background inside device outline
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        cardBg,
                                        accent.copy(alpha = 0.15f),
                                        Color.Black
                                    )
                                )
                            )
                    ) {
                        // Notch speaker mock
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                .background(Color(0xFF2E2E33))
                                .align(Alignment.TopCenter)
                        )

                        // Dynamic Left Edge Zone highlighting Size & Opacity Custom values
                        val previewWidth = (settings.buttonSizeDp.toFloat() / 90f * 24f).dp
                        val previewAlpha = settings.buttonOpacity

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(previewWidth)
                                .alpha(previewAlpha)
                                .align(Alignment.CenterStart)
                                .background(accent.copy(alpha = 0.3f))
                                .border(BorderStroke(1.dp, accent), RoundedCornerShape(0.dp))
                        )

                        // Dynamic Right Edge Zone highlighting Size & Opacity Custom values
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(previewWidth)
                                .alpha(previewAlpha)
                                .align(Alignment.CenterEnd)
                                .background(accent.copy(alpha = 0.3f))
                                .border(BorderStroke(1.dp, accent), RoundedCornerShape(0.dp))
                        )

                        // Center content label inside smartphone preview
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Swipe,
                                "Swipe",
                                tint = accent.copy(alpha = previewAlpha.coerceAtLeast(0.4f)),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Text(
                    "Left/Right borders highlight edge gesture ranges",
                    fontSize = 10.sp,
                    color = subtext,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Theme Selections Options
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Aesthetic & Visual Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("MINIMAL", "GAMIFIED", "ELITE").forEach { themeOption ->
                        val isSelected = settings.themeName == themeOption
                        val optAccent = getThemeAccentColor(themeOption)
                        val optionBg = getThemeCardBackgroundColor(themeOption)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) optAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)
                                )
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isSelected) optAccent else Color.Transparent
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.updateTheme(themeOption) }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = themeOption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) optAccent else text.copy(alpha = 0.6f),
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (themeOption) {
                                    "MINIMAL" -> "Sleek slate"
                                    "ELITE" -> "Retro green"
                                    else -> "Playful pink"
                                },
                                fontSize = 9.sp,
                                color = subtext,
                                textAlign = TextAlign.Center,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        // Dimensional Customization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Dimensions & Opacity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                // Slider 1: Dimension
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Edge Trigger Zone Range",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = text,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            "${settings.buttonSizeDp} dp (40-90)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                    Slider(
                        value = settings.buttonSizeDp.toFloat(),
                        onValueChange = { viewModel.updateButtonSize(it.toInt()) },
                        valueRange = 40f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = accent.copy(alpha = 0.15f)
                        )
                    )
                }

                // Slider 2: Opacity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Resting Alpha Visibility",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = text,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            "${(settings.buttonOpacity * 100).toInt()}% (20-100)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                    Slider(
                        value = settings.buttonOpacity,
                        onValueChange = { viewModel.updateButtonOpacity(it) },
                        valueRange = 0.2f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = accent.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        // Save & Shortcuts Setup Checklist
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Customize Panel Shortcuts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    "Assign virtual tasks displayed inside the floating helper overlays when tapped on your screen edges:",
                    fontSize = 11.sp,
                    color = subtext,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                val shortcutsConfig = listOf(
                    "BACK" to "Virtual edge 'Back' trigger",
                    "HOME" to "Virtual 'Home' screen shortcut",
                    "RECENTS" to "Open Recents apps list overlay",
                    "NOTIFICATIONS" to "Simulate Notification drawer pull down",
                    "QUICK_SETTINGS" to "Access quick system configuration tuner",
                    "LOCK_SCREEN" to "Put device screen off to sleep mode",
                    "VOLUME_UP" to "Digital raise system volume booster",
                    "VOLUME_DOWN" to "Digital lower system volume"
                )

                val activeList = settings.enabledShortcuts.split(",")

                Column {
                    shortcutsConfig.forEach { item ->
                        val isChecked = activeList.contains(item.first)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleShortcut(item.first) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { viewModel.toggleShortcut(item.first) },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    item.first,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = text,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                                Text(
                                    item.second,
                                    fontSize = 10.sp,
                                    color = subtext,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = accent.copy(alpha = 0.15f))

                // Action Haptic tactile toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleHaptic(!settings.hapticEnabled) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Tactile Vibration feedback",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = text,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            "Vibrate briefly on active coordinate taps",
                            fontSize = 10.sp,
                            color = subtext,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                    Switch(
                        checked = settings.hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accent,
                            checkedTrackColor = accent.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatsPane(
    stats: List<StatsEntity>,
    viewModel: MainViewModel,
    theme: String
) {
    val accent = getThemeAccentColor(theme)
    val cardBg = getThemeCardBackgroundColor(theme)
    val text = getThemeTextColor(theme)
    val subtext = getThemeSubtextColor(theme)

    val totalClicks = stats.sumOf { it.count }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Core counter impact card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HARDWARE INTEGRITY RATING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    text = "$totalClicks",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = text,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    text = "Total mechanical clicks saved over damaged button hardware!",
                    fontSize = 12.sp,
                    color = subtext,
                    textAlign = TextAlign.Center,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Custom Canvas based distribution metric bar layouts
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Usage Metrics Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = text,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                if (totalClicks == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No activities registered yet.\nTry triggering simulated commands in the Dashboard sandbox!",
                            fontSize = 12.sp,
                            color = subtext,
                            textAlign = TextAlign.Center,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                } else {
                    val sortedStats = stats.filter { it.count >= 0 }.sortedByDescending { it.count }
                    val maxItem = (sortedStats.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

                    sortedStats.forEach { stat ->
                        val ratio = stat.count.toFloat() / maxItem

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stat.actionName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = text,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                                Text(
                                    "${stat.count} Saved",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }

                            // Horizontal Canvas styled metrics track bar
                            val barColor = accent

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                            ) {
                                val width = size.width * ratio
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(width, size.height),
                                    cornerRadius = CornerRadius(7f, 7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = accent.copy(alpha = 0.15f))

                Button(
                    onClick = { viewModel.clearStats() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C1C1C),
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Delete, "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Reset Impact Log Statistics",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}
