package com.letsdrive.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.letsdrive.app.state.AppScreen

/**
 * Shell de navegação multiplataforma:
 * - Android (`androidMain`): Barra inferior padrão Android (Material 3 `NavigationBar`),
 *   com troca de aba apenas no toque normal (ao soltar o dedo), sem lente flutuante.
 * - iOS (`iosMain`): `UITabBarController` nativo do UIKit, que no iOS 26 entrega automaticamente
 *   o Liquid Glass nativo da Apple (exatamente o mesmo componente oficial do WhatsApp iOS e Tinder).
 */
@Composable
expect fun MainNavigationShell(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onPlusButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    screenContent: @Composable (AppScreen) -> Unit
)
