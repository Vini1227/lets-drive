package com.letsdrive.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.state.TripStateHolder
import com.letsdrive.app.ui.components.MainNavigationShell
import com.letsdrive.app.ui.screens.*
import com.letsdrive.app.ui.theme.FigmaDarkBg
import com.letsdrive.app.ui.theme.LetsDriveTheme

@Composable
fun App() {
    val stateHolder = remember { TripStateHolder() }
    val uiState by stateHolder.uiState.collectAsState()

    LetsDriveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FigmaDarkBg
        ) {
            when (uiState.currentScreen) {
                // Fluxo inicial sem barra inferior (Boas-vindas / Login / Cadastro)
                AppScreen.WELCOME -> {
                    WelcomeScreen(
                        onLoginClick = { stateHolder.navigateTo(AppScreen.LOGIN) },
                        onRegisterClick = { stateHolder.navigateTo(AppScreen.REGISTER) }
                    )
                }

                AppScreen.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = { username -> stateHolder.loginUser(username) },
                        onNavigateToRegister = { stateHolder.navigateTo(AppScreen.REGISTER) },
                        onBack = { stateHolder.navigateTo(AppScreen.WELCOME) },
                        errorMessage = uiState.authErrorMessage,
                        isLoading = uiState.isAuthLoading,
                        onLoginWithEmail = { email, pass ->
                            stateHolder.loginWithEmail(email, pass)
                        }
                    )
                }

                AppScreen.REGISTER -> {
                    RegisterScreen(
                        onRegisterSuccess = { name, vehicle ->
                            stateHolder.updateProfile(name, vehicle, uiState.userRole)
                            stateHolder.navigateTo(AppScreen.HOME)
                        },
                        onBackToLogin = { stateHolder.navigateTo(AppScreen.LOGIN) },
                        errorMessage = uiState.authErrorMessage,
                        isLoading = uiState.isAuthLoading,
                        onRegisterWithEmail = { name, email, vehicle, pass ->
                            stateHolder.registerWithEmail(name, email, vehicle, pass)
                        }
                    )
                }

                // Todas as telas autenticadas compartilham um único menu Liquid Glass com Backdrop Blur
                else -> {
                    MainNavigationShell(
                        currentScreen = uiState.currentScreen,
                        onSelectScreen = { screen -> stateHolder.navigateTo(screen) },
                        onPlusButtonClick = {
                            when (uiState.currentScreen) {
                                AppScreen.JOIN_CODE -> stateHolder.navigateTo(AppScreen.CONFIRM_ROUTE)
                                AppScreen.CONFIRM_ROUTE -> stateHolder.navigateTo(AppScreen.JOIN_CODE)
                                AppScreen.ROOM_CREATED -> stateHolder.navigateTo(AppScreen.SHARED_MAP)
                                else -> stateHolder.navigateTo(AppScreen.JOIN_CODE)
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    userName = uiState.userName,
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    onSearchDestinationClick = { stateHolder.navigateTo(AppScreen.CONFIRM_ROUTE) },
                                    onEnterCodeClick = { stateHolder.navigateTo(AppScreen.JOIN_CODE) },
                                    onOpenActiveMapClick = { stateHolder.navigateTo(AppScreen.SHARED_MAP) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.JOIN_CODE -> {
                                JoinCodeScreen(
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    defaultRole = uiState.userRole,
                                    onJoinTrip = { code, role ->
                                        stateHolder.joinTripWithCode(code, role)
                                    },
                                    onCreateNewRouteClick = { stateHolder.navigateTo(AppScreen.CONFIRM_ROUTE) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.CONFIRM_ROUTE -> {
                                ConfirmRouteScreen(
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    initialOrigin = uiState.currentTrip.originAddress,
                                    initialDestination = uiState.currentTrip.destinationName,
                                    initialMaxDistanceKm = uiState.maxSeparationDistanceKm,
                                    initialRole = uiState.userRole,
                                    onConfirmRoute = { origin, dest, maxDist, role ->
                                        stateHolder.confirmDestinationAndGenerateRoom(origin, dest, maxDist, role)
                                    },
                                    onBack = { stateHolder.navigateTo(AppScreen.HOME) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.ROOM_CREATED -> {
                                RoomCreatedScreen(
                                    trip = uiState.currentTrip,
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    onOpenMapClick = { stateHolder.navigateTo(AppScreen.SHARED_MAP) },
                                    onCancelTripClick = { stateHolder.navigateTo(AppScreen.HOME) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.SHARED_MAP -> {
                                SharedMapScreen(
                                    trip = uiState.currentTrip,
                                    isSharingLocation = uiState.isSharingLocation,
                                    isSimulatingSeparation = uiState.isSimulatingSeparation,
                                    focusedParticipantId = uiState.focusedParticipantId,
                                    confirmedMeetingPointName = uiState.confirmedMeetingPointName,
                                    isHardwareGpsActive = uiState.isHardwareGpsActive,
                                    onFocusParticipant = { id -> stateHolder.focusParticipant(id) },
                                    onToggleSimulateSeparation = { stateHolder.toggleRouteSeparationSimulation() },
                                    onToggleSharingLocation = { stateHolder.toggleLocationSharing(it) },
                                    onConfirmMeetingPoint = { mpName -> stateHolder.confirmMeetingPoint(mpName) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.HISTORY -> {
                                HistoryScreen(
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    recentTrips = uiState.recentTrips,
                                    onRepeatTripClick = { item ->
                                        stateHolder.confirmDestinationAndGenerateRoom(
                                            origin = item.originAddress,
                                            destination = item.destinationAddress,
                                            maxDistanceKm = uiState.maxSeparationDistanceKm,
                                            role = uiState.userRole
                                        )
                                    },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    userHandle = uiState.userHandle,
                                    userVehicle = uiState.userVehicle,
                                    maxSeparationDistanceKm = uiState.maxSeparationDistanceKm,
                                    isSharingLocation = uiState.isSharingLocation,
                                    isSupabaseConnected = uiState.isSupabaseConnected,
                                    supabaseProjectUrl = uiState.supabaseProjectUrl,
                                    onUpdateMaxDistance = { stateHolder.updateMaxSeparationThreshold(it) },
                                    onToggleSharingLocation = { stateHolder.toggleLocationSharing(it) },
                                    onUpdateSupabaseCredentials = { url, key -> stateHolder.updateSupabaseCredentials(url, key) },
                                    onLogoutClick = { stateHolder.navigateTo(AppScreen.WELCOME) },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            AppScreen.PROFILE -> {
                                ProfileScreen(
                                    userName = uiState.userName,
                                    userVehicle = uiState.userVehicle,
                                    userRole = uiState.userRole,
                                    onSaveProfile = { name, vehicle, role ->
                                        stateHolder.updateProfile(name, vehicle, role)
                                    },
                                    onNavigateMenu = { target -> stateHolder.navigateTo(target) }
                                )
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
