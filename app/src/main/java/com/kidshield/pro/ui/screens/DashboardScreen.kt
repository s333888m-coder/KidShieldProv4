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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidshield.pro.data.MonitoredApp
import com.kidshield.pro.ui.theme.AmberAccent
import com.kidshield.pro.ui.theme.CardElevated
import com.kidshield.pro.ui.theme.CardSurface
import com.kidshield.pro.ui.theme.CyanAccent
import com.kidshield.pro.ui.theme.DangerRed
import com.kidshield.pro.ui.theme.EmeraldAccent
import com.kidshield.pro.ui.theme.MidnightBg
import com.kidshield.pro.ui.theme.TextPrimary
import com.kidshield.pro.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    monitoredApps: List<MonitoredApp>,
    selectedPackage: String,
    installedApps: List<MonitoredApp>,
    timerMinutes: Int,
    timerSeconds: Int,
    soundName: String,
    afterTimerLabel: String,
    allPermissionsReady: Boolean,
    onSelectApp: (String) -> Unit,
    onAddApp: (MonitoredApp) -> Unit,
    onRemoveApp: (String) -> Unit,
    onTimerChanged: (Int, Int) -> Unit,
    onPickSound: () -> Unit,
    onTestSound: () -> Unit,
    onOpenSettings: () -> Unit,
    onActivateClick: () -> Unit,
    onLockAppNow: () -> Unit
) {
    var showAddAppDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var minText by remember(timerMinutes) { mutableStateOf(timerMinutes.toString()) }
    var secText by remember(timerSeconds) { mutableStateOf(timerSeconds.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyanAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = CyanAccent
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "KidShield Pro",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Parent Dashboard • पेरेंट डैशबोर्ड",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onLockAppNow) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock Dashboard",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = if (allPermissionsReady) CyanAccent else AmberAccent
                    )
                }
            }
        }

        if (!allPermissionsReady) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberAccent.copy(alpha = 0.14f))
                    .border(1.dp, AmberAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .clickable { onOpenSettings() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Setup Required: Overlay, Accessibility & Device Admin चालू करने के लिए Settings खोलें →",
                    color = AmberAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 1: Persistent Saved Apps List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kids Apps / सेव किए गए ऐप्स",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "एक बार ऐड करें, हमेशा सेव रहेगा",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                OutlinedButton(
                    onClick = { showAddAppDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add App",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            monitoredApps.forEach { app ->
                val isSelected = app.packageName == selectedPackage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) CyanAccent.copy(alpha = 0.14f) else CardElevated
                        )
                        .border(
                            1.dp,
                            if (isSelected) CyanAccent else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectApp(app.packageName) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) CyanAccent else Color.White.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                color = if (isSelected) MidnightBg else TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.appName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isSelected) "Selected for launch • चुना हुआ" else app.packageName,
                                color = if (isSelected) CyanAccent else TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (monitoredApps.size > 1) {
                            IconButton(onClick = { onRemoveApp(app.packageName) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove App",
                                    tint = DangerRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Custom Timer Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Set Timer / टाइमर सेट करें",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "10s (Demo)" to (0 to 10),
                    "5 min" to (5 to 0),
                    "15 min" to (15 to 0),
                    "30 min" to (30 to 0),
                    "60 min" to (60 to 0)
                )
                presets.forEach { (label, pair) ->
                    val active = timerMinutes == pair.first && timerSeconds == pair.second
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) CyanAccent else CardElevated)
                            .clickable {
                                minText = pair.first.toString()
                                secText = pair.second.toString()
                                onTimerChanged(pair.first, pair.second)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (active) MidnightBg else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { v ->
                        val clean = v.filter { it.isDigit() }.take(3)
                        minText = clean
                        onTimerChanged(clean.toIntOrNull() ?: 0, secText.toIntOrNull() ?: 0)
                    },
                    label = { Text("Minutes / मिनट") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = secText,
                    onValueChange = { v ->
                        val clean = v.filter { it.isDigit() }.take(2)
                        val clamped = (clean.toIntOrNull() ?: 0).coerceAtMost(59)
                        secText = if (clean.isEmpty()) "" else clamped.toString()
                        onTimerChanged(minText.toIntOrNull() ?: 0, clamped)
                    },
                    label = { Text("Seconds / सेकंड") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Custom Alert Sound & After-Timer Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = EmeraldAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lock Alert Sound / अलर्ट साउंड",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = soundName,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row {
                    IconButton(onClick = onTestSound) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Test Sound",
                            tint = CyanAccent
                        )
                    }
                    TextButton(onClick = onPickSound) {
                        Text("Choose", color = EmeraldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "After-Timer Lock Protection:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "$afterTimerLabel ›",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Primary ACTIVE CTA Button
        Button(
            onClick = onActivateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(EmeraldAccent, CyanAccent)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MidnightBg,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE • लॉक चालू करें",
                        color = MidnightBg,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "ACTIVE दबाते ही KidShield लॉक हो जाएगा और चुना हुआ ऐप (जैसे YouTube) खुल जाएगा। टाइमर खत्म होते ही साउंड बजेगा और Fake PIN लॉक लग जाएगा।",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    // Add App Modal Dialog
    if (showAddAppDialog) {
        val existingPkgs = monitoredApps.map { it.packageName }.toSet()
        val filtered = installedApps.filter {
            !existingPkgs.contains(it.packageName) &&
                (searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true))
        }

        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Add App to List / ऐप जोड़ें",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddApp(item)
                                        showAddAppDialog = false
                                        searchQuery = ""
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.appName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = item.packageName,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAppDialog = false }) {
                    Text("Close", color = CyanAccent)
                }
            }
        )
    }
}
