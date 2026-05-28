package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.LocalRepository
import com.example.data.SettingsEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class AssistiveTouchService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: LocalRepository

    private var floatingComposeView: ComposeView? = null
    private var leftEdgeComposeView: ComposeView? = null
    private var rightEdgeComposeView: ComposeView? = null
    private var panelComposeView: ComposeView? = null

    private var floatingParams = WindowManager.LayoutParams()
    private var leftEdgeParams = WindowManager.LayoutParams()
    private var rightEdgeParams = WindowManager.LayoutParams()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var floatingViewTreeOwner: ServiceViewTreeOwner? = null
    private var leftEdgeViewTreeOwner: ServiceViewTreeOwner? = null
    private var rightEdgeViewTreeOwner: ServiceViewTreeOwner? = null
    private var panelViewTreeOwner: ServiceViewTreeOwner? = null

    // Cached settings to apply to overlays
    private var currentSettings = SettingsEntity()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val db = AppDatabase.getInstance(this)
        repository = LocalRepository(db.appDao())
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure default layout parameters for floating overlay window
        floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 500
        }

        // Configure layout parameters for Left Edge Gesture trigger zone
        leftEdgeParams = WindowManager.LayoutParams(
            24, // placeholder, dynamically updated
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        // Configure layout parameters for Right Edge Gesture trigger zone
        rightEdgeParams = WindowManager.LayoutParams(
            24, // placeholder, dynamically updated
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 0
        }

        // Initialize floating button view tree owner
        val owner = ServiceViewTreeOwner().apply { start() }
        floatingViewTreeOwner = owner

        // Initialize floating button view tree
        floatingComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
        }

        // Initialize Left Edge view tree owner
        val leftOwner = ServiceViewTreeOwner().apply { start() }
        leftEdgeViewTreeOwner = leftOwner

        // Initialize Left Edge view tree
        leftEdgeComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(leftOwner)
            setViewTreeViewModelStoreOwner(leftOwner)
            setViewTreeSavedStateRegistryOwner(leftOwner)
        }

        // Initialize Right Edge view tree owner
        val rightOwner = ServiceViewTreeOwner().apply { start() }
        rightEdgeViewTreeOwner = rightOwner

        // Initialize Right Edge view tree
        rightEdgeComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(rightOwner)
            setViewTreeViewModelStoreOwner(rightOwner)
            setViewTreeSavedStateRegistryOwner(rightOwner)
        }

        // Add to WindowManager
        try {
            windowManager.addView(floatingComposeView, floatingParams)
            windowManager.addView(leftEdgeComposeView, leftEdgeParams)
            windowManager.addView(rightEdgeComposeView, rightEdgeParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for setting updates
        serviceScope.launch {
            repository.settingsFlow.collectLatest { settings ->
                val safeSettings = settings ?: SettingsEntity()
                currentSettings = safeSettings
                updateFloatingButtonUI(safeSettings)
                updateLeftEdgeUI(safeSettings)
                updateRightEdgeUI(safeSettings)
            }
        }
    }

    private fun updateLeftEdgeUI(settings: SettingsEntity) {
        val view = leftEdgeComposeView ?: return
        val theme = settings.themeName
        val accent = getThemeAccentColor(theme)
        val opacity = settings.buttonOpacity
        val edgeWidthPx = (settings.buttonSizeDp * resources.displayMetrics.density * 0.35f).toInt().coerceAtLeast(15)

        leftEdgeParams.width = edgeWidthPx
        try {
            windowManager.updateViewLayout(view, leftEdgeParams)
        } catch (e: Exception) {}

        view.setContent {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((settings.buttonSizeDp * 0.35f).dp)
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                if (totalDragX > 25f) {
                                    triggerVibrationIfNeeded()
                                    showActionPanel()
                                } else if (kotlin.math.abs(totalDragX) < 15f) {
                                    triggerVibrationIfNeeded()
                                    performSystemAction("BACK")
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                            }
                        )
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = 0.35f * opacity),
                                accent.copy(alpha = 0.1f * opacity),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }

    private fun updateRightEdgeUI(settings: SettingsEntity) {
        val view = rightEdgeComposeView ?: return
        val theme = settings.themeName
        val accent = getThemeAccentColor(theme)
        val opacity = settings.buttonOpacity
        val edgeWidthPx = (settings.buttonSizeDp * resources.displayMetrics.density * 0.35f).toInt().coerceAtLeast(15)

        rightEdgeParams.width = edgeWidthPx
        try {
            windowManager.updateViewLayout(view, rightEdgeParams)
        } catch (e: Exception) {}

        view.setContent {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((settings.buttonSizeDp * 0.35f).dp)
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                if (totalDragX < -25f) {
                                    triggerVibrationIfNeeded()
                                    showActionPanel()
                                } else if (kotlin.math.abs(totalDragX) < 15f) {
                                    triggerVibrationIfNeeded()
                                    performSystemAction("HOME")
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                            }
                        )
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.1f * opacity),
                                accent.copy(alpha = 0.35f * opacity)
                            )
                        )
                    )
            )
        }
    }

    private fun updateFloatingButtonUI(settings: SettingsEntity) {
        val view = floatingComposeView ?: return
        view.setContent {
            val size = settings.buttonSizeDp.dp
            val opacity = settings.buttonOpacity
            val theme = settings.themeName

            Box(
                modifier = Modifier
                    .size(size)
                    .alpha(opacity)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        var isDragging = false
                        var totalDrag = 0f
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                totalDrag = 0f
                            },
                            onDragEnd = {
                                if (totalDrag < 15f) {
                                    triggerVibrationIfNeeded()
                                    showActionPanel()
                                } else {
                                    snapToNearestEdge(size.value.toInt())
                                }
                            },
                            onDragCancel = {
                                snapToNearestEdge(size.value.toInt())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                                floatingParams.x = (floatingParams.x + dragAmount.x.toInt()).coerceAtLeast(0)
                                floatingParams.y = (floatingParams.y + dragAmount.y.toInt()).coerceAtLeast(0)
                                try {
                                    windowManager.updateViewLayout(floatingComposeView, floatingParams)
                                } catch (e: Exception) {}
                            }
                        )
                    }
                    .background(getThemeBrush(theme))
                    .border(
                        BorderStroke(
                            2.dp,
                            if (theme == "ELITE") Color(0xFF00FF66) else Color.White.copy(alpha = 0.6f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (theme) {
                    "MINIMAL" -> {
                        Box(
                            modifier = Modifier
                                .size(size / 3)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    "ELITE" -> {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Menu",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(size / 2)
                        )
                    }
                    else -> { // "GAMIFIED"
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                }
            }
        }
    }

    private fun getThemeBrush(theme: String): Brush {
        return when (theme) {
            "MINIMAL" -> Brush.verticalGradient(listOf(Color(0xFF2E2E2E), Color(0xFF141414)))
            "ELITE" -> Brush.verticalGradient(listOf(Color(0xFF0B140E), Color(0xFF020503)))
            else -> Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))) // Gamified purple-pink
        }
    }

    private fun snapToNearestEdge(btnSizePx: Int) {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val finalX = if (floatingParams.x + btnSizePx / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - btnSizePx - 40 // simple offset padding from the edge
        }

        val startX = floatingParams.x
        serviceScope.launch {
            val duration = 200
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= duration) {
                    floatingParams.x = finalX
                    try {
                        windowManager.updateViewLayout(floatingComposeView, floatingParams)
                    } catch (e: Exception) {}
                    break
                }
                val t = elapsed.toFloat() / duration
                val easeOut = 1f - (1f - t) * (1f - t)
                floatingParams.x = (startX + (finalX - startX) * easeOut).toInt()
                try {
                    windowManager.updateViewLayout(floatingComposeView, floatingParams)
                } catch (e: Exception) {}
                delay(16)
            }
        }
    }

    private fun showActionPanel() {
        if (panelComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val owner = ServiceViewTreeOwner().apply { start() }
        panelViewTreeOwner = owner

        panelComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
        }

        panelComposeView?.setContent {
            val theme = currentSettings.themeName
            val haptic = currentSettings.hapticEnabled
            val shortcuts = currentSettings.enabledShortcuts.split(",")

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .pointerInput(Unit) {
                        // Clicking backdrop closes the overlay
                        detectDragGestures(onDrag = { _, _ -> }, onDragEnd = { hideActionPanel() })
                    }
                    .clickable { hideActionPanel() },
                contentAlignment = Alignment.Center
            ) {
                // Interactive Dialog Panel
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp)
                        .clickable(enabled = false) {}
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            when (theme) {
                                "MINIMAL" -> Color(0xFF16161A)
                                "ELITE" -> Color(0xFF060A07)
                                else -> Color(0xFF151025)
                            }
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                when (theme) {
                                    "MINIMAL" -> Color(0xFFABC8F7).copy(alpha = 0.5f)
                                    "ELITE" -> Color(0xFF00FF66)
                                    else -> Color(0xFFD495FF).copy(alpha = 0.5f)
                                }
                            ),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title header
                        Text(
                            text = when (theme) {
                                "MINIMAL" -> "NAVIGATION"
                                "ELITE" -> "SYSTEM OVERRIDE v2.0"
                                else -> "Savior Touch Panel"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (theme) {
                                "MINIMAL" -> Color(0xFFABC8F7)
                                "ELITE" -> Color(0xFF00FF66)
                                else -> Color(0xFFD495FF)
                            },
                            fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default,
                            textAlign = TextAlign.Center
                        )

                        Divider(
                            color = when (theme) {
                                "ELITE" -> Color(0xFF00FF66).copy(alpha = 0.3f)
                                "MINIMAL" -> Color(0xFFABC8F7).copy(alpha = 0.15f)
                                else -> Color(0xFFD495FF).copy(alpha = 0.15f)
                            },
                            thickness = 1.dp
                        )

                        // Grid layout of shortcuts
                        val actionsList = listAllShortcutActions().filter { shortcuts.contains(it.id) }

                        if (actionsList.isEmpty()) {
                            Text(
                                "No active shortcuts in settings",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            val rows = actionsList.chunked(3)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (row in rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (action in row) {
                                            ActionButtonItem(
                                                id = action.id,
                                                label = action.label,
                                                icon = action.icon,
                                                theme = theme,
                                                onClick = {
                                                    triggerVibrationIfNeeded()
                                                    performSystemAction(action.id)
                                                    hideActionPanel()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Close bar
                        Button(
                            onClick = { hideActionPanel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (theme) {
                                    "MINIMAL" -> Color(0xFF2E2E35)
                                    "ELITE" -> Color(0xFF1E3523)
                                    else -> Color(0xFF3B1E5C)
                                },
                                contentColor = when (theme) {
                                    "MINIMAL" -> Color(0xFFABC8F7)
                                    "ELITE" -> Color(0xFF00FF66)
                                    else -> Color(0xFFD495FF)
                                }
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                "Minimize Panel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(panelComposeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun ActionButtonItem(
        id: String,
        label: String,
        icon: ImageVector,
        theme: String,
        onClick: () -> Unit
    ) {
        val shape = RoundedCornerShape(20.dp)
        val containerColor = when (theme) {
            "MINIMAL" -> Color(0xFF282830)
            "ELITE" -> Color(0xFF060D08)
            else -> Color(0xFF251A3C)
        }
        val iconColor = when (theme) {
            "MINIMAL" -> Color(0xFFABC8F7)
            "ELITE" -> Color(0xFF00FF66)
            else -> Color(0xFFD495FF)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(84.dp)
                .height(88.dp)
                .clip(shape)
                .background(containerColor)
                .border(
                    BorderStroke(
                        1.dp,
                        when (theme) {
                            "MINIMAL" -> Color(0xFFABC8F7).copy(alpha = 0.15f)
                            "ELITE" -> Color(0xFF00FF66).copy(alpha = 0.4f)
                            else -> Color(0xFFD495FF).copy(alpha = 0.15f)
                        }
                    ),
                    shape
                )
                .clickable { onClick() }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (theme == "ELITE") Color(0xFF00FF66) else Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                fontFamily = if (theme == "ELITE") FontFamily.Monospace else FontFamily.Default
            )
        }
    }

    private fun hideActionPanel() {
        val view = panelComposeView ?: return
        panelComposeView = null
        val owner = panelViewTreeOwner
        panelViewTreeOwner = null
        owner?.stop()
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibrationIfNeeded() {
        try {
            if (!currentSettings.hapticEnabled) return
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(40)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performSystemAction(actionId: String) {
        val globalAction = when (actionId) {
            "BACK" -> GLOBAL_ACTION_BACK
            "HOME" -> GLOBAL_ACTION_HOME
            "RECENTS" -> GLOBAL_ACTION_RECENTS
            "NOTIFICATIONS" -> GLOBAL_ACTION_NOTIFICATIONS
            "QUICK_SETTINGS" -> GLOBAL_ACTION_QUICK_SETTINGS
            "POWER_DIALOG" -> GLOBAL_ACTION_POWER_DIALOG
            "LOCK_SCREEN" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    -1
                }
            }
            else -> -1
        }

        if (globalAction != -1) {
            performGlobalAction(globalAction)
            serviceScope.launch {
                repository.incrementStat(actionId)
            }
        } else {
            // Adjust volume or system settings using AudioManager
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (actionId) {
                "VOLUME_UP" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    serviceScope.launch { repository.incrementStat("VOLUME_UP") }
                }
                "VOLUME_DOWN" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    serviceScope.launch { repository.incrementStat("VOLUME_DOWN") }
                }
            }
        }
    }

    private data class ShortcutInfo(val id: String, val label: String, val icon: ImageVector)

    private fun listAllShortcutActions(): List<ShortcutInfo> {
        return listOf(
            ShortcutInfo("BACK", "Back", Icons.AutoMirrored.Filled.ArrowBack),
            ShortcutInfo("HOME", "Home", Icons.Default.Home),
            ShortcutInfo("RECENTS", "Recents", Icons.Default.Menu),
            ShortcutInfo("NOTIFICATIONS", "Notification", Icons.Default.Notifications),
            ShortcutInfo("QUICK_SETTINGS", "Quick Settings", Icons.Default.Tune),
            ShortcutInfo("LOCK_SCREEN", "Lock Screen", Icons.Default.Lock),
            ShortcutInfo("VOLUME_UP", "Vol Up", Icons.Default.VolumeUp),
            ShortcutInfo("VOLUME_DOWN", "Vol Down", Icons.Default.VolumeDown),
            ShortcutInfo("POWER_DIALOG", "Power", Icons.Default.PowerSettingsNew)
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Required, can remain empty for overlay-only functionality
    }

    override fun onInterrupt() {
        // Required
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()

        hideActionPanel()

        floatingViewTreeOwner?.stop()
        floatingViewTreeOwner = null

        leftEdgeViewTreeOwner?.stop()
        leftEdgeViewTreeOwner = null

        rightEdgeViewTreeOwner?.stop()
        rightEdgeViewTreeOwner = null

        val fView = floatingComposeView
        floatingComposeView = null
        if (fView != null) {
            try {
                windowManager.removeView(fView)
            } catch (e: Exception) {}
        }

        val lView = leftEdgeComposeView
        leftEdgeComposeView = null
        if (lView != null) {
            try {
                windowManager.removeView(lView)
            } catch (e: Exception) {}
        }

        val rView = rightEdgeComposeView
        rightEdgeComposeView = null
        if (rView != null) {
            try {
                windowManager.removeView(rView)
            } catch (e: Exception) {}
        }
    }
}
