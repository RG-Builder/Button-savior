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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

            // Theme adaptation inside the main application itself - fully consistent with the select theme!
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
        darkTheme = (themeName == "ELITE") || isSystemInDarkTheme(),
        dynamicColor = false,
        content = content
    )
}

fun getThemeBackgroundColor(theme: String): Color {
    return when (theme) {
        "MINIMAL" -> Color(0xFFFAFAFA)
        "ELITE" -> Color(0xFF020503) // Hack style pure obsidian
        else -> Color(0xFFF9FAFB) // Gamified light clean background with bright overlays
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            color = getThemeAccentColor(theme).copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Animated Screen view switcher
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (theme == "ELITE") Color(0xFF09110B) else accent.copy(alpha = 0.08f)
            )
            .border(
                BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (theme) {
                "MINIMAL" -> Icons.Default.Circle
                "ELITE" -> Icons.Default.Code
                else -> Icons.Default.TouchApp
            },
            contentDescription = "App Icon",
            tint = accent,
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Button Savior",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (theme == "ELITE") Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )
            Text(
                text = "Alternative Touch overlay for broken buttons",
                fontSize = 12.sp,
                color = Color.Gray,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (theme == "ELITE") Color(0xFF070B08) else Color(0xFFF1F5F9)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            if (theme == "ELITE") Color(0xFF142418) else accent
                        } else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        if (theme == "ELITE") Color(0xFF00FF66) else Color.White
                    } else {
                        Color.Gray
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

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Accessibility Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (theme == "ELITE") {
                        if (isServiceActive) Color(0xFF0E2214) else Color(0xFF1F0D0D)
                    } else {
                        if (isServiceActive) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                    }
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (isServiceActive) Color(0xFF10B981) else Color(0xFFEF4444)
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pulse = rememberInfiniteTransition(label = "pulse")
                    val alphaState by pulse.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulser"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(alphaState)
                            .clip(CircleShape)
                            .background(if (isServiceActive) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceActive) "SERVICE STATUS: ACTIVE" else "SERVICE STATUS: INACTIVE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isServiceActive) Color(0xFF047857) else Color(0xFFB91C1C),
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }

                Text(
                    text = if (isServiceActive) {
                        "Your virtual touch-based navigator overlay is currently drawing over other screens. It is ready to receive taps!"
                    } else {
                        "The navigation overlay helper requires Accessibility permissions so it can perform system actions (Back, Home, Recents) on your behalf."
                    },
                    fontSize = 12.sp,
                    color = if (theme == "ELITE") Color.Gray else Color.DarkGray,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceActive) Color(0xFF10B981) else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceActive) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceActive) "Go to Accessibility Settings" else "Enable Savior Overlay Service",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }

        // Live Preview of custom floating button appearance
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (theme == "ELITE") Color(0xFF060B07) else Color(0xFFF8FAFC)
                )
                .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Visual Overlay Preview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Selected Theme: $theme (Size: ${settings.buttonSizeDp}dp, Opacity: ${(settings.buttonOpacity * 100).toInt()}%)",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Sandbox simulated touch panel box so they can click buttons (simulating triggers and incrementing counters)
        InteractiveSandboxPanel(theme = theme, viewModel = viewModel, shortcuts = settings.enabledShortcuts)
    }
}

@Composable
fun InteractiveSandboxPanel(theme: String, viewModel: MainViewModel, shortcuts: String) {
    val accent = getThemeAccentColor(theme)
    val shortcutList = shortcuts.split(",")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (theme == "ELITE") Color(0xFF050805) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                text = "Tap any simulator button to test overlay trigger animations and immediately feed the Impact Statistics dashboard with usage counts!",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Simulating button options
            val testButtons = listOf(
                Triple("BACK", "Back Click", Icons.AutoMirrored.Filled.ArrowBack),
                Triple("HOME", "Home Click", Icons.Default.Home),
                Triple("RECENTS", "Recents Click", Icons.Default.Menu),
                Triple("NOTIFICATIONS", "Notification", Icons.Default.Notifications),
                Triple("LOCK_SCREEN", "Screen Lock", Icons.Default.Lock),
                Triple("VOLUME_UP", "Raise Vol", Icons.Default.VolumeUp)
            )

            var lastSimulatedAction by remember { mutableStateOf<String?>(null) }

            val rows = testButtons.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (btn in row) {
                            val isConfigured = shortcutList.contains(btn.first)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (theme == "ELITE") Color(0xFF0E1A11) else Color(0xFFF1F5F9)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isConfigured) accent.copy(alpha = 0.5f) else Color.Transparent
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.simulateActionClick(btn.first)
                                        lastSimulatedAction = btn.second
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = btn.third,
                                    contentDescription = btn.second,
                                    tint = if (isConfigured) accent else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    btn.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (theme == "ELITE") Color(0xFF00FF66) else Color.DarkGray,
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
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.1f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Simulated action triggered: $action! (+1 Impact Stat registered)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme == "ELITE") Color(0xFF00FF66) else Color.DarkGray,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
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

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Theme Selector Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (theme == "ELITE") Color(0xFF050805) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MINIMAL", "GAMIFIED", "ELITE").forEach { themeOption ->
                        val isSelected = settings.themeName == themeOption
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        if (themeOption == "ELITE") Color(0xFF142418) else getThemeAccentColor(themeOption).copy(alpha = 0.15f)
                                    } else {
                                        if (theme == "ELITE") Color(0xFF0D120E) else Color(0xFFF8FAFC)
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isSelected) getThemeAccentColor(themeOption) else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.updateTheme(themeOption) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = themeOption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) getThemeAccentColor(themeOption) else Color.Gray,
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
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        // Overlay Button Size & Opacity Customizer Slider Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (theme == "ELITE") Color(0xFF050805) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Dimensions & Opacity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                // Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Floating Button Size",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (theme == "ELITE") Color.Gray else Color.DarkGray,
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
                            activeTrackColor = accent
                        )
                    )
                }

                // Opacity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Resting Opacity Transparency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (theme == "ELITE") Color.Gray else Color.DarkGray,
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
                            activeTrackColor = accent
                        )
                    )
                }
            }
        }

        // Action Panel Shortcuts Customizer Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (theme == "ELITE") Color(0xFF050805) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Save & Customize Virtual Shortcuts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    "Activate or deactivate virtual shortcuts displayed inside the floating panel overlay when tapped:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                val shortcutsConfig = listOf(
                    "BACK" to "Virtual 'Back' touch",
                    "HOME" to "Virtual 'Home' touch",
                    "RECENTS" to "Virtual 'Recents' touch",
                    "NOTIFICATIONS" to "Pull Notification Drawer",
                    "QUICK_SETTINGS" to "Settings Tuner Panel",
                    "LOCK_SCREEN" to "Screen Off lock shortcut",
                    "VOLUME_UP" to "Digital sound louder",
                    "VOLUME_DOWN" to "Digital sound quiet"
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    item.first,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (theme == "ELITE") Color.LightGray else Color.Black,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                                Text(
                                    item.second,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                // Vibration Haptics Toggle Row
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
                            "Tactile Vibration Feedback",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme == "ELITE") Color.LightGray else Color.Black,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                        Text(
                            "Produce micro vibration on active finger tap",
                            fontSize = 10.sp,
                            color = Color.Gray,
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

    // Calculate total clicks saved on physical buttons
    val totalClicks = stats.sumOf { it.count }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Core counter impact card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (theme == "ELITE") Color(0xFF0C130D) else accent.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HARDWARE IMPACT INTEGRITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    text = "$totalClicks",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = if (theme == "ELITE") Color(0xFF00FF66) else Color.DarkGray,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )

                Text(
                    text = "Total physical clicks saved on your broken hardware buttons!",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Custom Canvas based bar charts
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (theme == "ELITE") Color(0xFF050805) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Usage Metrics Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
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
                            "No activities registered yet.\nTry triggering simulator views on Dashboard!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                        )
                    }
                } else {
                    // Render simple custom bars in Canvas!
                    val sortedStats = stats.filter { it.count > 0 }.sortedByDescending { it.count }
                    val maxItem = sortedStats.maxOfOrNull { it.count } ?: 1

                    sortedStats.forEach { stat ->
                        val ratio = stat.count.toFloat() / maxItem

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stat.actionName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (theme == "ELITE") Color.LightGray else Color.Black,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                                Text(
                                    "${stat.count} clicks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                                )
                            }

                            // Dynamic styled custom bars using Compose Canvas
                            val barColor = when (theme) {
                                "MINIMAL" -> Color.DarkGray
                                "ELITE" -> Color(0xFF00FF66)
                                else -> Color(0xFF8B5CF6)
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (theme == "ELITE") Color(0xFF0B140E) else Color(0xFFF1F5F9)
                                    )
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

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                Button(
                    onClick = { viewModel.clearStats() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "ELITE") Color(0xFF230D0D) else Color(0xFFFEF2F2),
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, "Clear")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Reset Impact Log Statistics",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}

fun getThemeAccentColor(theme: String): Color {
    return when (theme) {
        "MINIMAL" -> Color(0xFF2E2E2E)
        "ELITE" -> Color(0xFF00FF66) // Neon hacker green
        else -> Color(0xFF9C27B0) // Gamified vibrant orchid purple
    }
}
