package com.letsdrive.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.model.HistoricalTripItem
import com.letsdrive.app.model.ParticipantRole
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.ui.components.FigmaTopProfileBar
import com.letsdrive.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Tela 8: Frame 30:82 do Figma ("Viagens Recentes" com cards de Viagem Concluída e Viagem Cancelada)
 * Sem álbum de fotos, focado 100% em histórico de rotas e compartilhamento (Sugestão do Professor).
 */
@Composable
fun HistoryScreen(
    userHandle: String,
    userVehicle: String,
    recentTrips: List<HistoricalTripItem>,
    onRepeatTripClick: (HistoricalTripItem) -> Unit,
    onRefreshHistory: () -> Unit = {},
    onNavigateMenu: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            FigmaTopProfileBar(
                userName = userHandle,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = true,
                onBackClick = { onNavigateMenu(AppScreen.HOME) },
                onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Viagens Recentes",
                    color = FigmaTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRefreshHistory() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar histórico",
                        tint = FigmaTealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Atualizar",
                        color = FigmaTealPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            recentTrips.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 21.dp, vertical = 10.dp)
                ) {
                    // Data no canto superior direito (ex: "02/11/26" igual ao node 30:86)
                    Text(
                        text = if (item.inviteCode.isNotBlank()) "${item.dateLabel} • ${item.inviteCode}" else item.dateLabel,
                        color = FigmaTextSubtle,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 6.dp, bottom = 4.dp)
                    )

                    // Card bg white 16% opacity, rounded 7px, height 85px (igual ao Rectangle 21 do Figma)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable { onRepeatTripClick(item) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Linha 1: Origem + Tag de Status (Concluída / Cancelada)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = item.originAddress,
                                        color = FigmaTextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (item.isCompleted) "Viagem Concluída" else "Viagem Cancelada",
                                        color = FigmaTextWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (item.isCompleted) Color.White else FigmaDangerRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Linha 2: Destino com pin verde-água (#10CEAB)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WhereToVote,
                                    contentDescription = null,
                                    tint = FigmaTealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = item.destinationAddress,
                                    color = FigmaTextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tela 9: Aba "Configurações" do Menu Inferior (Raio Geoespacial de Alerta, Compartilhamento GPS e Sair)
 */
@Composable
fun SettingsScreen(
    userHandle: String,
    userVehicle: String,
    maxSeparationDistanceKm: Double,
    isSharingLocation: Boolean,
    areNotificationsEnabled: Boolean = true,
    isSupabaseConnected: Boolean = false,
    supabaseProjectUrl: String = "",
    onUpdateMaxDistance: (Double) -> Unit,
    onToggleSharingLocation: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit = {},
    onTestNotification: () -> Unit = {},
    onUpdateSupabaseCredentials: (url: String, key: String) -> Unit = { _, _ -> },
    onLogoutClick: () -> Unit,
    onNavigateMenu: (AppScreen) -> Unit
) {
    var sliderVal by remember(maxSeparationDistanceKm) { mutableStateOf(maxSeparationDistanceKm.toFloat()) }
    var showDbDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(supabaseProjectUrl) }
    var keyInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            FigmaTopProfileBar(
                userName = userHandle,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = true,
                onBackClick = { onNavigateMenu(AppScreen.HOME) },
                onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Configurações de Sincronização",
                    color = FigmaTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ajuste os alertas geoespaciais e permissões de localização.",
                    color = FigmaTextMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(22.dp))

                // Card 1: Distância Máxima antes da Notificação (Sugestão do Professor)
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaSheetBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distância Máxima p/ Alerta",
                                color = FigmaTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(sliderVal * 10).roundToInt() / 10.0} km",
                                color = FigmaNeonCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Usa consultas geoespaciais (PostGIS / MongoDB 2dsphere) para disparar um alerta assim que a distância entre participantes passar deste valor.",
                            color = FigmaTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = sliderVal,
                            onValueChange = {
                                sliderVal = it
                                onUpdateMaxDistance((it * 10).roundToInt() / 10.0)
                            },
                            valueRange = 0.5f..15.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = FigmaTealPrimary,
                                activeTrackColor = FigmaTealPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Card 2: Compartilhamento de Localização em Tempo Real
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaSheetBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compartilhar Minha Posição",
                                color = FigmaTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Permite que seus amigos em viagem ou aguardando você em casa acompanhem seu deslocamento.",
                                color = FigmaTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isSharingLocation,
                            onCheckedChange = onToggleSharingLocation,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FigmaTealPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Card: Notificações Nativas de Alerta
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaSheetBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notificações Nativas de Alerta",
                                    color = FigmaTextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Receba alertas do sistema quando algum participante ultrapassar a distância máxima ou confirmar ponto de reencontro.",
                                    color = FigmaTextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = areNotificationsEnabled,
                                onCheckedChange = onToggleNotifications,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = FigmaTealPrimary
                                )
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onTestNotification,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FigmaOrangeLogo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = FigmaOrangeLogo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Disparar Notificação de Teste",
                                color = FigmaOrangeLogo,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Card 3: Status da Conexão Supabase PostGIS (Backend Real)
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaSheetBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Banco Geoespacial (PostGIS)",
                                color = FigmaTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSupabaseConnected) FigmaMintLogo else FigmaOrangeLogo)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isSupabaseConnected) "Conectado" else "Modo Local",
                                    color = if (isSupabaseConnected) FigmaMintLogo else FigmaOrangeLogo,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isSupabaseConnected)
                                "Conectado ao Supabase com extensões PostGIS ativas (geography(Point, 4326) e ST_DWithin)."
                            else
                                "Operando em modo simulado/local com dados do schema.sql. Toque para conectar um projeto Supabase.",
                            color = FigmaTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showDbDialog = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, FigmaTealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSupabaseConnected) "Configurar Conexão" else "Conectar Projeto Supabase",
                                color = FigmaTealPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (showDbDialog) {
                    AlertDialog(
                        onDismissRequest = { showDbDialog = false },
                        containerColor = FigmaSheetBg,
                        title = {
                            Text("Conectar Supabase PostGIS", color = FigmaTextWhite, fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column {
                                Text(
                                    "Insira a URL e a Anon Key do seu projeto Supabase para ativar consultas geoespaciais em tempo real:",
                                    color = FigmaTextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    label = { Text("Project URL (https://xyz.supabase.co)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = keyInput,
                                    onValueChange = { keyInput = it },
                                    label = { Text("Anon Public Key (eyJ...)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onUpdateSupabaseCredentials(urlInput, keyInput)
                                    showDbDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaTealPrimary)
                            ) {
                                Text("Salvar e Conectar", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDbDialog = false }) {
                                Text("Cancelar", color = FigmaTextMuted)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FigmaSecondaryButtonBg)
                        .clickable { onLogoutClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sair da Conta",
                        color = FigmaTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tela 10: Aba "Você" do Menu Inferior (Editar Nome, Veículo ou Modo Parado/Aguardando em Casa)
 */
@Composable
fun ProfileScreen(
    userName: String,
    userVehicle: String,
    userRole: ParticipantRole,
    onSaveProfile: (name: String, vehicle: String, role: ParticipantRole) -> Unit,
    onNavigateMenu: (AppScreen) -> Unit
) {
    var nameInput by remember(userName) { mutableStateOf(userName) }
    var vehicleInput by remember(userVehicle) { mutableStateOf(userVehicle) }
    var roleInput by remember(userRole) { mutableStateOf(userRole) }
    var savedBanner by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FigmaTopProfileBar(
                userName = userName,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = true,
                onBackClick = { onNavigateMenu(AppScreen.HOME) },
                onProfileClick = {}
            )

            Spacer(Modifier.height(14.dp))

            // Avatar Grande
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Seu Nome no Mapa",
                    color = FigmaTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        savedBanner = false
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Identificação (Ex: Fiat Uno, A pé, Em Casa)",
                    color = FigmaTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vehicleInput,
                    onValueChange = {
                        vehicleInput = it
                        savedBanner = false
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Modo de Uso Principal (Sugestão do Professor)",
                    color = FigmaTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                ParticipantRole.entries.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (roleInput == role) FigmaTealPrimary.copy(alpha = 0.2f) else FigmaSheetBg
                            )
                            .clickable {
                                roleInput = role
                                if (role == ParticipantRole.STATIONARY_HOST && vehicleInput == "Fiat Uno") {
                                    vehicleInput = "Em Casa (Aguardando)"
                                }
                            }
                            .padding(12.dp)
                    ) {
                        RadioButton(
                            selected = roleInput == role,
                            onClick = { roleInput = role },
                            colors = RadioButtonDefaults.colors(selectedColor = FigmaTealPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = role.label,
                            color = FigmaTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(FigmaTealPrimary)
                        .clickable {
                            onSaveProfile(nameInput, vehicleInput, roleInput)
                            savedBanner = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Salvar Perfil",
                        color = FigmaTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (savedBanner) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Perfil atualizado com sucesso!",
                        color = FigmaTealPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
