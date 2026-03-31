package cn.tr1ck.matrixclock.ui.screen

import cn.tr1ck.matrixclock.data.model.AuthRecord
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Dialog background: lighter than main (black) so it stands out
private val DialogBg = Color(0xFF252B33)
// Card/item surface inside dialog: one step lighter for hierarchy
private val DialogCardBg = Color(0xFF2D3748)
// Primary actions: bright so they pop on dialog bg
private val DialogAccent = Color(0xFF0EA5E9)
private val DialogText = Color(0xFFF1F5F9)
private val DialogTextDim = Color(0xFF94A3B8)

@Composable
fun SettingsDialog(
    ipAddress: String?,
    isWebControlEnabled: Boolean,
    onToggleWebControl: (Boolean) -> Unit,
    is24HourFormat: Boolean,
    onToggle24HourFormat: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClearAuth: () -> Unit,
    authRecords: List<AuthRecord>,
    onRevokeAuth: (String) -> Unit,
    onOpenAuthManagement: () -> Unit,
    onOpenAdminInBrowser: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DialogBg,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFF475569))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Settings",
                    color = DialogText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                val adminLabel = if (ipAddress != null) "$ipAddress:6574" else "NO WIFI"
                Text(
                    text = adminLabel,
                    color = if (ipAddress != null) DialogAccent else DialogTextDim,
                    fontSize = 14.sp,
                    fontWeight = if (ipAddress != null) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .then(
                            if (ipAddress != null) Modifier.clickable { onOpenAdminInBrowser() }
                            else Modifier
                        )
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Web Control Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DialogCardBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Web Control",
                                color = DialogText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Enable remote control via browser",
                                color = DialogTextDim,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isWebControlEnabled,
                            onCheckedChange = onToggleWebControl,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DialogAccent,
                                checkedTrackColor = DialogAccent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DialogCardBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "24-hour clock",
                                color = DialogText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Turn off to show AM/PM in all clock templates",
                                color = DialogTextDim,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = is24HourFormat,
                            onCheckedChange = onToggle24HourFormat,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DialogAccent,
                                checkedTrackColor = DialogAccent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.clickable { onOpenAuthManagement() },
                    shape = RoundedCornerShape(12.dp),
                    color = DialogCardBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authorization management",
                                color = DialogText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${authRecords.size} device(s) authorized. View and revoke.",
                                color = DialogTextDim,
                                fontSize = 12.sp
                            )
                        }
                        Text(color = DialogAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium, text = "Open")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.clickable { onClearAuth() },
                    shape = RoundedCornerShape(12.dp),
                    color = DialogCardBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Clear all authorizations",
                            color = DialogText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Revoke all devices from web control",
                            color = DialogTextDim,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DialogAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AuthManagementDialog(
    authRecords: List<AuthRecord>,
    onRevoke: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DialogBg,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFF475569))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 320.dp)
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = "Authorization management",
                    color = DialogText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Authorized devices. Tap to revoke.",
                    color = DialogTextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (authRecords.isEmpty()) {
                        Text(
                            text = "No authorized devices",
                            color = DialogTextDim,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        authRecords.forEach { record ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = DialogCardBg
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.deviceName.ifBlank { record.deviceModel.ifBlank { "Device" } },
                                            color = DialogText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = buildString {
                                                if (record.deviceModel.isNotBlank()) append(record.deviceModel)
                                                if (record.systemVersion.isNotBlank()) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append(record.systemVersion)
                                                }
                                                if (record.batteryLevel >= 0) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append("${record.batteryLevel}%")
                                                }
                                                if (record.ip.isNotBlank()) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append(record.ip)
                                                }
                                                if (isEmpty()) append("—")
                                            },
                                            color = DialogTextDim,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    TextButton(
                                        onClick = { onRevoke(record.token) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Text("Revoke", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DialogAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AuthDialog(ip: String, onAllow: () -> Unit, onDeny: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DialogBg,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFF475569))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp)
            ) {
                Text(
                    text = "Allow connection",
                    color = DialogText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DialogCardBg
                ) {
                    Text(
                        text = ip,
                        color = DialogAccent,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authorize this device to control the matrix?",
                    color = DialogTextDim,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DialogText),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF64748B))
                    ) {
                        Text("Deny", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onAllow,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DialogAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Allow", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
