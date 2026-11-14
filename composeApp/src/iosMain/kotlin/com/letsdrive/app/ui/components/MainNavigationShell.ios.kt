package com.letsdrive.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.letsdrive.app.state.AppScreen
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
import platform.darwin.NSObject

private fun screenToIosTabIndex(screen: AppScreen): Int = when (screen) {
    AppScreen.HOME, AppScreen.SHARED_MAP -> 0
    AppScreen.HISTORY -> 1
    AppScreen.JOIN_CODE, AppScreen.CONFIRM_ROUTE, AppScreen.ROOM_CREATED -> 2
    AppScreen.SETTINGS -> 3
    AppScreen.PROFILE -> 4
    else -> 0
}

/**
 * Implementação iOS usando `UITabBarController` 100% nativo do UIKit.
 *
 * Ao compilar no Xcode 26 / iOS 26 no Mac, o sistema operacional aplica automaticamente
 * a barra flutuante oficial **Apple Liquid Glass** (a mesma usada pelo WhatsApp iOS e Tinder),
 * com refração real, física de toque/arraste oficial da Apple e troca apenas ao soltar o dedo.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MainNavigationShell(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onPlusButtonClick: () -> Unit,
    modifier: Modifier,
    screenContent: @Composable (AppScreen) -> Unit
) {
    val latestScreenState = rememberUpdatedState(currentScreen)
    val latestOnSelect = rememberUpdatedState(onSelectScreen)
    val latestOnPlus = rememberUpdatedState(onPlusButtonClick)
    val latestContent = rememberUpdatedState(screenContent)

    val delegate = remember {
        object : NSObject(), UITabBarControllerDelegateProtocol {
            override fun tabBarController(
                tabBarController: UITabBarController,
                didSelectViewController: UIViewController
            ) {
                when (tabBarController.selectedIndex.toInt()) {
                    0 -> latestOnSelect.value(AppScreen.HOME)
                    1 -> latestOnSelect.value(AppScreen.HISTORY)
                    2 -> latestOnPlus.value()
                    3 -> latestOnSelect.value(AppScreen.SETTINGS)
                    4 -> latestOnSelect.value(AppScreen.PROFILE)
                }
            }
        }
    }

    UIKitViewController(
        factory = {
            val tabBarController = UITabBarController()
            tabBarController.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
            tabBarController.tabBar.tintColor = UIColor.whiteColor
            tabBarController.delegate = delegate

            fun buildTabController(
                title: String,
                sfSymbol: String,
                selectedSfSymbol: String,
                tag: Long,
                defaultTabScreen: AppScreen,
                matchesActiveScreen: (AppScreen) -> Boolean
            ): UIViewController {
                val vc = ComposeUIViewController {
                    val active = latestScreenState.value
                    val screenToRender = if (matchesActiveScreen(active)) active else defaultTabScreen
                    latestContent.value(screenToRender)
                }
                vc.tabBarItem = UITabBarItem(
                    title = title,
                    image = UIImage.systemImageNamed(sfSymbol),
                    selectedImage = UIImage.systemImageNamed(selectedSfSymbol)
                ).apply {
                    this.tag = tag
                }
                return vc
            }

            val homeVc = buildTabController(
                title = "Página inicial",
                sfSymbol = "house",
                selectedSfSymbol = "house.fill",
                tag = 0L,
                defaultTabScreen = AppScreen.HOME,
                matchesActiveScreen = { it == AppScreen.HOME || it == AppScreen.SHARED_MAP }
            )
            val historyVc = buildTabController(
                title = "Histórico",
                sfSymbol = "clock.arrow.circlepath",
                selectedSfSymbol = "clock.arrow.circlepath",
                tag = 1L,
                defaultTabScreen = AppScreen.HISTORY,
                matchesActiveScreen = { it == AppScreen.HISTORY }
            )
            val plusVc = buildTabController(
                title = "Viagem",
                sfSymbol = "plus.circle",
                selectedSfSymbol = "plus.circle.fill",
                tag = 2L,
                defaultTabScreen = AppScreen.JOIN_CODE,
                matchesActiveScreen = {
                    it == AppScreen.JOIN_CODE ||
                        it == AppScreen.CONFIRM_ROUTE ||
                        it == AppScreen.ROOM_CREATED
                }
            )
            val settingsVc = buildTabController(
                title = "Configurações",
                sfSymbol = "gearshape",
                selectedSfSymbol = "gearshape.fill",
                tag = 3L,
                defaultTabScreen = AppScreen.SETTINGS,
                matchesActiveScreen = { it == AppScreen.SETTINGS }
            )
            val profileVc = buildTabController(
                title = "Você",
                sfSymbol = "person.crop.circle",
                selectedSfSymbol = "person.crop.circle.fill",
                tag = 4L,
                defaultTabScreen = AppScreen.PROFILE,
                matchesActiveScreen = { it == AppScreen.PROFILE }
            )

            tabBarController.setViewControllers(
                listOf(homeVc, historyVc, plusVc, settingsVc, profileVc),
                animated = false
            )
            tabBarController.selectedIndex = screenToIosTabIndex(latestScreenState.value).toULong()
            tabBarController
        },
        modifier = modifier.fillMaxSize(),
        update = { tabBarController ->
            val targetIdx = screenToIosTabIndex(currentScreen).toULong()
            if (tabBarController.selectedIndex != targetIdx) {
                tabBarController.selectedIndex = targetIdx
            }
        }
    )
}

