package com.letsdrive.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.ui.theme.FigmaDarkBg
import com.letsdrive.app.ui.theme.FigmaMintLogo
import com.letsdrive.app.ui.theme.FigmaTealPrimary
import com.letsdrive.app.ui.theme.FigmaTextMuted
import com.letsdrive.app.ui.theme.FigmaTextWhite

private data class AndroidNavTab(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val LeftAndroidTabs = listOf(
    AndroidNavTab(AppScreen.HOME, "Início", Icons.Filled.Home, Icons.Outlined.Home),
    AndroidNavTab(AppScreen.HISTORY, "Histórico", Icons.Filled.History, Icons.Outlined.History)
)

private val RightAndroidTabs = listOf(
    AndroidNavTab(AppScreen.SETTINGS, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
    AndroidNavTab(AppScreen.PROFILE, "Você", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

private fun isAndroidTabSelected(currentScreen: AppScreen, tabScreen: AppScreen): Boolean {
    return when (tabScreen) {
        AppScreen.HOME -> currentScreen == AppScreen.HOME || currentScreen == AppScreen.SHARED_MAP
        else -> currentScreen == tabScreen
    }
}

@Composable
actual fun MainNavigationShell(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onPlusButtonClick: () -> Unit,
    modifier: Modifier,
    screenContent: @Composable (AppScreen) -> Unit
) {
    val isPlusFlowActive = currentScreen == AppScreen.JOIN_CODE ||
        currentScreen == AppScreen.CONFIRM_ROUTE ||
        currentScreen == AppScreen.ROOM_CREATED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 160),
            label = "AndroidScreenTransition"
        ) { targetScreen ->
            screenContent(targetScreen)
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = Color.White.copy(alpha = 0.08f)
            )
            NavigationBar(
                containerColor = Color(0xFF161616),
                contentColor = FigmaTextWhite,
                tonalElevation = 0.dp
            ) {
                LeftAndroidTabs.forEach { tab ->
                    val selected = isAndroidTabSelected(currentScreen, tab.screen)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectScreen(tab.screen) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FigmaMintLogo,
                            selectedTextColor = FigmaTextWhite,
                            indicatorColor = FigmaTealPrimary.copy(alpha = 0.20f),
                            unselectedIconColor = FigmaTextMuted,
                            unselectedTextColor = FigmaTextMuted
                        )
                    )
                }

                // Botão central "+" padrão Android (acionado apenas no toque e soltura normal)
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isPlusFlowActive) FigmaTealPrimary else Color(0xFFE2E2E2))
                            .clickable(onClick = onPlusButtonClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Nova viagem ou código",
                            tint = if (isPlusFlowActive) Color.White else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                RightAndroidTabs.forEach { tab ->
                    val selected = isAndroidTabSelected(currentScreen, tab.screen)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectScreen(tab.screen) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FigmaMintLogo,
                            selectedTextColor = FigmaTextWhite,
                            indicatorColor = FigmaTealPrimary.copy(alpha = 0.20f),
                            unselectedIconColor = FigmaTextMuted,
                            unselectedTextColor = FigmaTextMuted
                        )
                    )
                }
            }
        }
    }
}

