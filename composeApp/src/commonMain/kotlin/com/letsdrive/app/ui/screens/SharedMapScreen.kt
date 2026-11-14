package com.letsdrive.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.letsdrive.app.model.ParticipantStatus
import com.letsdrive.app.model.Trip
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.ui.components.InteractiveRoadMapCanvas
import com.letsdrive.app.ui.theme.*

/**
 * Tela 7: iPhone 17 - 5 (Mapa Rota Neon + Bottom Sheet "Vocês estão indo para Praia da Palma, Alagoas"
 * + Alerta de Distância Máxima do Professor + Sugestão de Ponto de Encontro + Menu Inferior Glass)
 */
@Composable
fun SharedMapScreen(
    trip: Trip,
    isSharingLocation: Boolean,
    isSimulatingSeparation: Boolean,
    focusedParticipantId: String?,
    confirmedMeetingPointName: String? = null,
    isHardwareGpsActive: Boolean = false,
    onFocusParticipant: (String) -> Unit,
    onToggleSimulateSeparation: () -> Unit,
    onToggleSharingLocation: (Boolean) -> Unit,
    onConfirmMeetingPoint: (String) -> Unit = {},
    onCompleteTrip: () -> Unit = {},
    onCancelTrip: () -> Unit = {},
    onShareInvite: () -> Unit = {},
    onNavigateMenu: (AppScreen) -> Unit
) {
    var isSheetExpanded by remember { mutableStateOf(false) }
    var showMeetingPointModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Mapa Interativo Neon
        InteractiveRoadMapCanvas(
            participants = trip.participants,
            isSimulatingSeparation = isSimulatingSeparation,
            focusedParticipantId = focusedParticipantId,
            selectedMeetingPointName = trip.suggestedMeetingPoints.firstOrNull()?.name,
            destinationName = trip.destinationName.substringBefore(",").trim().ifBlank { "Destino" },
            originCoordinates = trip.originCoordinates,
            destinationCoordinates = trip.destinationCoordinates,
            intermediateStopName = trip.intermediateStopAddress,
            intermediateStopCoordinates = trip.intermediateStopCoordinates,
            routeGeometry = trip.routeGeometry,
            maxSeparationDistanceKm = trip.maxSeparationDistanceKm,
            meetingPointCoordinates = trip.suggestedMeetingPoints.firstOrNull()?.coordinates,
            onSelectParticipant = { id ->
                onFocusParticipant(id)
                isSheetExpanded = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Controles superiores discretos (Testar alerta de distância máxima do professor + Código da Sala)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge da Sala e Limite Geoespacial + Botão de Compartilhar Convite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FigmaDarkBg.copy(alpha = 0.88f))
                            .border(1.dp, FigmaTealPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { onNavigateMenu(AppScreen.ROOM_CREATED) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSharingLocation) FigmaTealPrimary else FigmaOrangeLogo)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${trip.inviteCode} • ${trip.maxSeparationDistanceKm}km" +
                                if (isHardwareGpsActive) " • GPS" else " • Sync",
                            color = FigmaTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FigmaDarkBg.copy(alpha = 0.88f))
                            .border(1.dp, FigmaTealPrimary.copy(alpha = 0.6f), CircleShape)
                            .clickable { onShareInvite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar convite da sala",
                            tint = FigmaTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Botão para testar o pop-up de afastamento máximo
                FilterChip(
                    selected = isSimulatingSeparation,
                    onClick = onToggleSimulateSeparation,
                    label = {
                        Text(
                            text = if (isSimulatingSeparation) "Simulando Desvio" else "Testar Afastamento",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = FigmaDarkBg.copy(alpha = 0.88f),
                        labelColor = FigmaTextWhite,
                        selectedContainerColor = FigmaOrangeLogo.copy(alpha = 0.25f),
                        selectedLabelColor = FigmaOrangeLogo,
                        selectedLeadingIconColor = FigmaOrangeLogo
                    )
                )
            }

            // Banner de Ponto de Reencontro Confirmado pelo Grupo
            AnimatedVisibility(visible = confirmedMeetingPointName != null) {
                if (confirmedMeetingPointName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FigmaDarkBg.copy(alpha = 0.92f))
                            .border(1.dp, FigmaTealPrimary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WhereToVote,
                            contentDescription = null,
                            tint = FigmaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Ponto de Reencontro Confirmado: $confirmedMeetingPointName",
                            color = FigmaTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Pop-up de Alerta quando alguém ultrapassa a distância máxima configurada (Sugestão do Professor)
            AnimatedVisibility(visible = trip.activeSeparationAlert != null) {
                val alert = trip.activeSeparationAlert
                if (alert != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1209).copy(alpha = 0.96f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .border(1.5.dp, FigmaOrangeLogo, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = FigmaOrangeLogo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "LIMITE DE ${alert.configuredThresholdKm} KM ULTRAPASSADO",
                                        color = FigmaOrangeLogo,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(
                                    text = "${alert.currentDistanceKm} km",
                                    color = FigmaTextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${alert.separatedParticipantName} se afastou do grupo. Deseja sugerir um ponto de reencontro à frente?",
                                color = FigmaTextWhite,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showMeetingPointModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaTealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ver Ponto de Reencontro Sugerido",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Sheet Escura (#0F0F0F, top radius 50px) fiel ao iPhone 17 - 5
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isSheetExpanded) 440.dp else 308.dp)
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                .background(FigmaDarkBg)
                .clickable { isSheetExpanded = !isSheetExpanded }
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .padding(bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Line 12 (Alça 53px branca)
            Box(
                modifier = Modifier
                    .width(53.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.8f))
            )

            Spacer(Modifier.height(18.dp))

            // Textos principais idênticos ao frame 1:451
            Text(
                text = "Vocês estão indo para",
                color = FigmaTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = trip.destinationName,
                color = FigmaTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light
            )
            if (!trip.intermediateStopAddress.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Parada: ${trip.intermediateStopAddress}",
                    color = FigmaTextWhite.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))

            // Resumo dos participantes (Em Movimento e Aguardando no Local)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trip.participants.forEach { p ->
                    val isSep = p.status == ParticipantStatus.SEPARATED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FigmaSheetBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${p.displayName} • ${p.vehicleLabel}",
                                color = FigmaTextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = p.routeNote ?: "",
                                color = if (isSep) FigmaOrangeLogo else FigmaTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = if (p.etaMinutes == 0) "No local" else "${p.etaMinutes} min",
                            color = if (isSep) FigmaOrangeLogo else FigmaNeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (trip.participants.size == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FigmaSheetBg.copy(alpha = 0.7f))
                            .border(1.dp, FigmaTealPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = FigmaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Aguardando outros participantes entrarem com o código ${trip.inviteCode}",
                            color = FigmaTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FigmaTealPrimary)
                            .clickable { onCompleteTrip() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Concluir Viagem",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FigmaSecondaryButtonBg)
                            .border(1.dp, FigmaDangerRed.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .clickable { onCancelTrip() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = FigmaDangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Cancelar / Sair",
                            color = FigmaTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Modal de Ponto de Reencontro quando acionado
        if (showMeetingPointModal) {
            val mp = trip.suggestedMeetingPoints.firstOrNull()
            if (mp != null) {
                AlertDialog(
                    onDismissRequest = { showMeetingPointModal = false },
                    containerColor = FigmaSheetBg,
                    title = {
                        Text(
                            text = mp.name,
                            color = FigmaTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Column {
                            Text(mp.address, color = FigmaTextMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = mp.aiAdvisoryNote,
                                color = FigmaTextWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            mp.participantEtas.forEach { eta ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(eta.participantName, color = FigmaTextLightGray, fontSize = 12.sp)
                                    Text(
                                        text = if (eta.etaMinutes == 0) "Já no destino" else "${eta.distanceKm} km • ${eta.etaMinutes} min",
                                        color = FigmaTealPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onConfirmMeetingPoint(mp.name)
                                showMeetingPointModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaTealPrimary)
                        ) {
                            Text("Confirmar Ponto no Mapa", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}
