package com.kidshield.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidshield.pro.data.PrefsManager
import com.kidshield.pro.ui.theme.AmberAccent
import com.kidshield.pro.ui.theme.CardElevated
import com.kidshield.pro.ui.theme.CardSurface
import com.kidshield.pro.ui.theme.CyanAccent
import com.kidshield.pro.ui.theme.EmeraldAccent
import com.kidshield.pro.ui.theme.MidnightBg
import com.kidshield.pro.ui.theme.TextPrimary
import com.kidshield.pro.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    afterTimerMinutes: Int,
    antiUninstallGuard: Boolean,
    autoForceStopEnabled: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasDeviceAdminPermission: Boolean,
    hasNotificationPermission: Boolean,
    onBack: () -> Unit,
    onSelectAfterTimerMinutes: (Int) -> Unit,
    onToggleAntiUninstallGuard: (Boolean) -> Unit,
    onToggleAutoForceStop: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Protection Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "आफ्टर-टाइमर, फोर्स-स्टॉप और डिवाइस एडमिन",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // After-Timer Lock Duration Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "After-Timer Protection Limit",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "टाइमर खत्म होने के बाद कितने समय तक Fake PIN लॉक रहेगा (सिर्फ फिंगरप्रिंट से ही खुलेगा):",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val options = listOf(
                15 to "15 Min / मिनट",
                30 to "30 Min / मिनट",
                60 to "1 Hour / घंटा",
                120 to "2 Hours / घंटे",
                PrefsManager.AFTER_TIMER_UNTIL_FP to "Until Fingerprint Only"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (mins, label) ->
                    val selected = afterTimerMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) EmeraldAccent else CardElevated)
                            .clickable { onSelectAfterTimerMinutes(mins) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) MidnightBg else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Auto Force-Stop Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto Force-Stop App (YouTube)",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "टाइमर खत्म होते ही Accessibility के ज़रिए YouTube को पूरी तरह Force Stop कर देगा (ऑडियो/वीडियो बंद)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = autoForceStopEnabled,
                onCheckedChange = onToggleAutoForceStop,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MidnightBg,
                    checkedTrackColor = CyanAccent
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Anti-Uninstall Guard Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anti-Uninstall Fingerprint Guard",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ऐप अनइंस्टॉल या एडमिन बंद करने पर फिंगरप्रिंट मांगेगा",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = antiUninstallGuard,
                onCheckedChange = onToggleAntiUninstallGuard,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MidnightBg,
                    checkedTrackColor = EmeraldAccent
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Required Android Permissions / ज़रूरी परमिशन",
            color = CyanAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            icon = Icons.Rounded.Layers,
            title = "Display Over Other Apps (Overlay)",
            subtitle = "YouTube के ऊपर 100% Solid Fake PIN लॉक दिखाने के लिए अनिवार्य",
            granted = hasOverlayPermission,
            onClick = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            icon = Icons.Rounded.Security,
            title = "Accessibility Service (Lock & Force-Stop)",
            subtitle = "टाइमर के बाद YouTube को Force Stop करने और अनइंस्टॉल रोकने के लिए",
            granted = hasAccessibilityPermission,
            onClick = onRequestAccessibility
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            icon = Icons.Rounded.AdminPanelSettings,
            title = "Device Administrator",
            subtitle = "मज़बूत Anti-Uninstall और सिस्टम प्रोटेक्शन के लिए",
            granted = hasDeviceAdminPermission,
            onClick = onRequestDeviceAdmin
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionCard(
            icon = Icons.Rounded.NotificationsActive,
            title = "Notifications Permission",
            subtitle = "बैकग्राउंड टाइमर सर्विस को बिना रुके चलाने के लिए",
            granted = hasNotificationPermission,
            onClick = onRequestNotification
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                1.dp,
                if (granted) EmeraldAccent.copy(alpha = 0.4f) else AmberAccent.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) EmeraldAccent else AmberAccent,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (granted) EmeraldAccent else AmberAccent
        )
    }
}
