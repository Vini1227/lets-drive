package com.letsdrive.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.model.Participant
import com.letsdrive.app.model.ParticipantRole
import com.letsdrive.app.model.ParticipantStatus
import com.letsdrive.app.ui.theme.*

@Composable
fun InteractiveRoadMapCanvas(
    participants: List<Participant>,
    isSimulatingSeparation: Boolean,
    focusedParticipantId: String?,
    selectedMeetingPointName: String?,
    onSelectParticipant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040D1A)) // Fundo azul-noite satelital igual ao iPhone 17 - 5
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Oceano Atlântico à direita (litoral de Maceió / Praia da Palma)
            val coastline = Path().apply {
                moveTo(w * 0.25f, h * 0.72f)
                quadraticTo(w * 0.55f, h * 0.45f, w * 0.92f, h * 0.34f)
                quadraticTo(w * 0.80f, h * 0.22f, w, h * 0.12f)
                lineTo(w, h)
                lineTo(w * 0.15f, h)
                close()
            }
            drawPath(
                path = coastline,
                color = Color(0xFF031930)
            )

            // Brilho dourado urbano de Maceió ao longo da orla (igual à arte do Figma)
            val urbanShore = Path().apply {
                moveTo(w * 0.28f, h * 0.68f)
                quadraticTo(w * 0.56f, h * 0.44f, w * 0.90f, h * 0.34f)
                quadraticTo(w * 0.82f, h * 0.24f, w * 0.96f, h * 0.15f)
            }
            drawPath(
                path = urbanShore,
                color = Color(0xFFF59E0B).copy(alpha = 0.28f),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // Malha rodoviária secundária (BR-101 ao fundo)
            val br101 = Path().apply {
                moveTo(0f, h * 0.46f)
                quadraticTo(w * 0.28f, h * 0.25f, w * 0.46f, 0f)
            }
            drawPath(
                path = br101,
                color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // Rota Principal Neon Ciano (#38F0FC) de "Você" -> "Lucas" -> "Ana" -> "Destino"
            val neonRoute = Path().apply {
                moveTo(w * 0.08f, h * 0.55f)
                cubicTo(
                    w * 0.26f, h * 0.49f,
                    w * 0.24f, h * 0.40f,
                    w * 0.35f, h * 0.39f
                )
                cubicTo(
                    w * 0.52f, h * 0.37f,
                    w * 0.55f, h * 0.33f,
                    w * 0.62f, h * 0.31f
                )
                cubicTo(
                    w * 0.75f, h * 0.29f,
                    w * 0.77f, h * 0.25f,
                    w * 0.84f, h * 0.23f
                )
            }

            // Brilho externo Neon (Glow)
            drawPath(
                path = neonRoute,
                color = FigmaNeonCyan.copy(alpha = 0.28f),
                style = Stroke(width = glowPulse, cap = StrokeCap.Round)
            )
            // Brilho médio
            drawPath(
                path = neonRoute,
                color = FigmaNeonCyan.copy(alpha = 0.55f),
                style = Stroke(width = 14f, cap = StrokeCap.Round)
            )
            // Linha central Neon #38F0FC
            drawPath(
                path = neonRoute,
                color = FigmaNeonCyan,
                style = Stroke(width = 6.5f, cap = StrokeCap.Round)
            )

            // Rota de desvio quando "Simular Desvio" está ativo (Lucas ultrapassa o raio máximo configurado)
            if (isSimulatingSeparation) {
                val deviationPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.44f)
                    quadraticTo(w * 0.20f, h * 0.24f, w * 0.72f, h * 0.26f)
                }
                drawPath(
                    path = deviationPath,
                    color = FigmaOrangeLogo.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                    )
                )
            }
        }

        // Placa da rodovia "BR-101" igual à imagem do Figma
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 115.dp, top = 160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("BR-101", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Rótulo "Maceió" na costa
        Text(
            text = "Maceió",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 52.dp, bottom = 65.dp)
        )

        // Pin de Destino Final Neon Ciano no canto superior direito
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 175.dp, end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FigmaNeonCyan.copy(alpha = 0.25f))
                    .border(2.dp, FigmaNeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Destino",
                    tint = FigmaNeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF06152D).copy(alpha = 0.9f))
                    .border(1.dp, FigmaNeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Praia da Palma", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Marcadores dos participantes no estilo exato do iPhone 17 - 5 (ícone redondo de carro + pílula lateral com nome)
        participants.forEach { p ->
            val positionModifier = when (p.id) {
                "u1" -> Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp, top = 75.dp)
                "u2" -> {
                    if (isSimulatingSeparation) {
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 78.dp, top = 215.dp)
                    } else {
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 115.dp, bottom = 75.dp)
                    }
                }
                else -> Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 105.dp, top = 248.dp)
            }

            val isSeparated = p.status == ParticipantStatus.SEPARATED
            val ringColor = when {
                isSeparated -> FigmaOrangeLogo
                p.id == "u3" -> Color(0xFF8B5CF6) // Roxo/Lilás para "Ana" como no Figma iPhone 17 - 5
                else -> FigmaNeonCyan
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = positionModifier.clickable { onSelectParticipant(p.id) }
            ) {
                // Círculo com ícone do carro (ou Casa quando parado esperando)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06152D))
                        .border(2.2.dp, ringColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (p.role == ParticipantRole.STATIONARY_HOST) {
                            Icons.Default.Home
                        } else {
                            Icons.Default.DirectionsCar
                        },
                        contentDescription = p.displayName,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Etiqueta arredondada com o nome ("Você", "Lucas", "Ana")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06152D).copy(alpha = 0.92f))
                        .border(1.dp, ringColor.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = p.displayName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSeparated) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Afastado",
                                tint = FigmaOrangeLogo,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
