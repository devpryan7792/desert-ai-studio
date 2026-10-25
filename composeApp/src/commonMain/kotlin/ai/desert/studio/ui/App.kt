package ai.desert.studio.ui

import ai.desert.studio.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ScreenTab {
    STUDIO,
    QUICK_TOOLS,
    VAULT
}

@Composable
fun App() {
    DesertStudioTheme {
        var currentTab by remember { mutableStateOf(ScreenTab.STUDIO) }
        val vaultItems = remember { mutableStateListOf<VaultItem>() }

        fun saveItem(title: String, content: String) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
            vaultItems.add(
                0,
                VaultItem(
                    id = "vault_${System.currentTimeMillis()}",
                    title = title,
                    content = content,
                    timestamp = timeStr
                )
            )
        }

        Scaffold(
            containerColor = BgDark,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = AccentPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("D", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Desert AI Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero-Cloud On-Device Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Offline status indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceElevated,
                        modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentPrimary)
                            )
                            Text(
                                text = "Local Inference • 0 B Cloud",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.STUDIO,
                        onClick = { currentTab = ScreenTab.STUDIO },
                        icon = { Text("⚡", fontSize = 18.sp) },
                        label = { Text("Studio Pipelines", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.QUICK_TOOLS,
                        onClick = { currentTab = ScreenTab.QUICK_TOOLS },
                        icon = { Text("🛠", fontSize = 18.sp) },
                        label = { Text("Quick Tools", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.VAULT,
                        onClick = { currentTab = ScreenTab.VAULT },
                        icon = { Text("🗄", fontSize = 18.sp) },
                        label = {
                            Text(
                                if (vaultItems.isEmpty()) "Vault" else "Vault (${vaultItems.size})",
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BgDark)
            ) {
                when (currentTab) {
                    ScreenTab.STUDIO -> PipelineStudioView(
                        onSaveToVault = { title, content -> saveItem(title, content) }
                    )
                    ScreenTab.QUICK_TOOLS -> QuickToolsView(
                        onSaveToVault = { title, content -> saveItem(title, content) }
                    )
                    ScreenTab.VAULT -> VaultView(
                        items = vaultItems,
                        onClearAll = { vaultItems.clear() }
                    )
                }
            }
        }
    }
}
