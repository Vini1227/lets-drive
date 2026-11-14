package com.letsdrive.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.model.LatLng
import com.letsdrive.app.model.Participant
import com.letsdrive.app.model.ParticipantRole
import com.letsdrive.app.model.ParticipantStatus
import com.letsdrive.app.ui.theme.*
import kotlin.math.*

private const val EARTH_CIRCUMFERENCE_METERS = 40_075_016.686
private const val MIN_MAP_ZOOM = 8.0f
private const val MAX_MAP_ZOOM = 17.5f

private fun lonToMercatorX(lon: Double): Double = (lon + 180.0) / 360.0

private fun latToMercatorY(lat: Double): Double {
    val clamped = lat.coerceIn(-85.05112878, 85.05112878)
    val rad = clamped * (PI / 180.0)
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0
}

private fun mercatorXToLon(xNorm: Double): Double {
    return (xNorm * 360.0 - 180.0).coerceIn(-180.0, 180.0)
}

private fun mercatorYToLat(yNorm: Double): Double {
    val n = PI - 2.0 * PI * yNorm.coerceIn(0.0, 1.0)
    return ((180.0 / PI) * atan(sinh(n))).coerceIn(-85.0, 85.0)
}

private fun projectToScreen(
    point: LatLng,
    cameraLat: Double,
    cameraLon: Double,
    zoom: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    density: Float
): Offset {
    val worldScale = 256.0 * density.toDouble() * 2.0.pow(zoom.toDouble())
    val camX = lonToMercatorX(cameraLon) * worldScale
    val camY = latToMercatorY(cameraLat) * worldScale
    val ptX = lonToMercatorX(point.longitude) * worldScale
    val ptY = latToMercatorY(point.latitude) * worldScale
    return Offset(
        x = (viewportWidthPx * 0.5 + (ptX - camX)).toFloat(),
        y = (viewportHeightPx * 0.39 + (ptY - camY)).toFloat()
    )
}

private fun metersPerPixelAtLat(lat: Double, zoom: Float, density: Float): Double {
    val worldScale = 256.0 * density.toDouble() * 2.0.pow(zoom.toDouble())
    return ((EARTH_CIRCUMFERENCE_METERS * cos(lat * (PI / 180.0))) / worldScale).coerceAtLeast(0.01)
}

private fun computeAutoFitCamera(
    points: List<LatLng>,
    viewportSize: IntSize,
    density: Float
): Triple<Double, Double, Float> {
    if (points.isEmpty()) return Triple(-9.58, -35.69, 12.2f)
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0

    val w = viewportSize.width.takeIf { it > 0 }?.toDouble() ?: 390.0 * density
    val h = viewportSize.height.takeIf { it > 0 }?.toDouble() ?: 800.0 * density
    val spanX = (lonToMercatorX(maxLon) - lonToMercatorX(minLon)).coerceAtLeast(0.00004)
    val spanY = abs(latToMercatorY(minLat) - latToMercatorY(maxLat)).coerceAtLeast(0.00004)

    val zoomX = log2((w * 0.70) / (256.0 * density * spanX))
    val zoomY = log2((h * 0.42) / (256.0 * density * spanY))
    val fittedZoom = minOf(zoomX, zoomY).toFloat().coerceIn(MIN_MAP_ZOOM, 15.8f)
    return Triple(centerLat, centerLon, fittedZoom)
}

@Composable
fun InteractiveRoadMapCanvas(
    participants: List<Participant>,
    isSimulatingSeparation: Boolean,
    focusedParticipantId: String?,
    selectedMeetingPointName: String?,
    destinationName: String = "Destino",
    originCoordinates: LatLng = LatLng(-9.6498, -35.7089),
    destinationCoordinates: LatLng = LatLng(-9.5120, -35.6810),
    intermediateStopName: String? = null,
    intermediateStopCoordinates: LatLng? = null,
    routeGeometry: List<LatLng> = emptyList(),
    maxSeparationDistanceKm: Double = 2.0,
    meetingPointCoordinates: LatLng? = null,
    onSelectParticipant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    var cameraLat by remember { mutableStateOf((originCoordinates.latitude + destinationCoordinates.latitude) / 2.0) }
    var cameraLon by remember { mutableStateOf((originCoordinates.longitude + destinationCoordinates.longitude) / 2.0) }
    var mapZoom by remember { mutableStateOf(12.2f) }
    var hasInitializedCamera by remember { mutableStateOf(false) }

    val currentParticipants by rememberUpdatedState(participants)
    val currentOrigin by rememberUpdatedState(originCoordinates)
    val currentDest by rememberUpdatedState(destinationCoordinates)
    val currentStop by rememberUpdatedState(intermediateStopCoordinates)
    val currentGeometry by rememberUpdatedState(routeGeometry)
    val currentMeetingPoint by rememberUpdatedState(meetingPointCoordinates)

    fun recenterToFullRoute() {
        val allPoints = buildList {
            add(currentOrigin)
            add(currentDest)
            currentStop?.let { add(it) }
            addAll(currentGeometry)
            addAll(currentParticipants.map { it.location })
            currentMeetingPoint?.let { add(it) }
        }
        val (lat, lon, z) = computeAutoFitCamera(allPoints, viewportSize, density)
        cameraLat = lat
        cameraLon = lon
        mapZoom = z
    }

    // Ajusta o enquadramento quando o tamanho da tela ou a rota principal mudar
    LaunchedEffect(viewportSize, originCoordinates, destinationCoordinates, intermediateStopCoordinates) {
        if (viewportSize.width > 0 && viewportSize.height > 0 && !hasInitializedCamera) {
            recenterToFullRoute()
            hasInitializedCamera = true
        }
    }

    LaunchedEffect(originCoordinates, destinationCoordinates, intermediateStopCoordinates) {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            recenterToFullRoute()
        }
    }

    // Centraliza no participante focado ao selecionar no mapa ou lista
    LaunchedEffect(focusedParticipantId) {
        val target = participants.firstOrNull { it.id == focusedParticipantId }
        if (target != null && hasInitializedCamera) {
            cameraLat = target.location.latitude
            cameraLon = target.location.longitude
            if (mapZoom < 13.0f) mapZoom = 13.5f
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val effectiveRoute = remember(routeGeometry, originCoordinates, destinationCoordinates) {
        if (routeGeometry.size >= 2) routeGeometry else listOf(originCoordinates, destinationCoordinates)
    }

    val leaderLocation = remember(participants, originCoordinates) {
        participants.firstOrNull { it.isCurrentUser && it.role != ParticipantRole.STATIONARY_HOST }?.location
            ?: participants.firstOrNull { it.role != ParticipantRole.STATIONARY_HOST }?.location
            ?: originCoordinates
    }

    val coastlineCoords = remember {
        listOf(
            LatLng(-9.860, -35.920),
            LatLng(-9.760, -35.825),
            LatLng(-9.675, -35.720),
            LatLng(-9.620, -35.695),
            LatLng(-9.540, -35.610),
            LatLng(-9.450, -35.530),
            LatLng(-9.300, -35.400),
            LatLng(-9.010, -35.210)
        )
    }

    val regionalArterialCorridors = remember {
        listOf(
            // Corredor Litorâneo AL-101 Norte / Sul
            listOf(
                LatLng(-9.850, -35.910),
                LatLng(-9.770, -35.840),
                LatLng(-9.670, -35.714),
                LatLng(-9.615, -35.696),
                LatLng(-9.512, -35.681),
                LatLng(-9.440, -35.545),
                LatLng(-9.012, -35.222)
            ),
            // Corredor BR-101 / BR-104 (Sertão & Aeroporto)
            listOf(
                LatLng(-9.750, -35.920),
                LatLng(-9.649, -35.760),
                LatLng(-9.565, -35.785),
                LatLng(-9.510, -35.791),
                LatLng(-9.350, -35.770)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040D1A))
            .onSizeChanged { viewportSize = it }
    ) {
        // Camada base interativa do mapa (Canvas geográfico Web Mercator + gestos de Pan & Pinch Zoom)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewportSize) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val nextZoom = (mapZoom + log2(gestureZoom.coerceIn(0.5f, 2.0f))).coerceIn(
                            MIN_MAP_ZOOM,
                            MAX_MAP_ZOOM
                        )
                        val worldScale = 256.0 * density.toDouble() * 2.0.pow(nextZoom.toDouble())
                        val camX = lonToMercatorX(cameraLon) * worldScale - pan.x.toDouble()
                        val camY = latToMercatorY(cameraLat) * worldScale - pan.y.toDouble()
                        cameraLon = mercatorXToLon(camX / worldScale)
                        cameraLat = mercatorYToLat(camY / worldScale)
                        mapZoom = nextZoom
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            fun toScreen(pt: LatLng): Offset = projectToScreen(
                point = pt,
                cameraLat = cameraLat,
                cameraLon = cameraLon,
                zoom = mapZoom,
                viewportWidthPx = w,
                viewportHeightPx = h,
                density = density
            )

            // 1. Grade cartográfica ancorada em coordenadas geográficas reais
            val gridStepDeg = when {
                mapZoom >= 14.5f -> 0.005
                mapZoom >= 12.5f -> 0.02
                mapZoom >= 10.5f -> 0.05
                else -> 0.10
            }
            val worldScale = 256.0 * density.toDouble() * 2.0.pow(mapZoom.toDouble())
            val centerWorldX = lonToMercatorX(cameraLon) * worldScale
            val centerWorldY = latToMercatorY(cameraLat) * worldScale
            val minLon = mercatorXToLon((centerWorldX - w * 0.6) / worldScale)
            val maxLon = mercatorXToLon((centerWorldX + w * 0.6) / worldScale)
            val maxLat = mercatorYToLat((centerWorldY - h * 0.6) / worldScale)
            val minLat = mercatorYToLat((centerWorldY + h * 0.7) / worldScale)

            val startLatTick = floor(minLat / gridStepDeg) * gridStepDeg
            var latTick = startLatTick
            var safetyCount = 0
            while (latTick <= maxLat && safetyCount < 80) {
                val pStart = toScreen(LatLng(latTick, minLon))
                val pEnd = toScreen(LatLng(latTick, maxLon))
                drawLine(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.24f),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 1.2f
                )
                latTick += gridStepDeg
                safetyCount++
            }

            val startLonTick = floor(minLon / gridStepDeg) * gridStepDeg
            var lonTick = startLonTick
            safetyCount = 0
            while (lonTick <= maxLon && safetyCount < 80) {
                val pStart = toScreen(LatLng(maxLat, lonTick))
                val pEnd = toScreen(LatLng(minLat, lonTick))
                drawLine(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.24f),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 1.2f
                )
                lonTick += gridStepDeg
                safetyCount++
            }

            // 2. Oceano Atlântico e brilho urbano costeiro ancorados em LatLng (região de Alagoas)
            if (cameraLat in -10.8..-8.0 && cameraLon in -36.8..-34.5) {
                val oceanPath = Path().apply {
                    val firstOcean = toScreen(coastlineCoords.first())
                    moveTo(firstOcean.x, firstOcean.y)
                    coastlineCoords.drop(1).forEach { pt ->
                        val off = toScreen(pt)
                        lineTo(off.x, off.y)
                    }
                    lineTo(w * 2.5f, -h)
                    lineTo(w * 2.5f, h * 2.5f)
                    lineTo(-w, h * 2.5f)
                    close()
                }
                drawPath(path = oceanPath, color = Color(0xFF031930))

                val shoreGlowPath = Path().apply {
                    val first = toScreen(coastlineCoords.first())
                    moveTo(first.x, first.y)
                    coastlineCoords.drop(1).forEach { pt ->
                        val off = toScreen(pt)
                        lineTo(off.x, off.y)
                    }
                }
                drawPath(
                    path = shoreGlowPath,
                    color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 3. Eixos rodoviários secundários ancorados geograficamente
            regionalArterialCorridors.forEach { road ->
                val roadPath = Path().apply {
                    val p0 = toScreen(road.first())
                    moveTo(p0.x, p0.y)
                    road.drop(1).forEach { pt ->
                        val off = toScreen(pt)
                        lineTo(off.x, off.y)
                    }
                }
                drawPath(
                    path = roadPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.22f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Raio geodésico máximo de separação (ST_DWithin) projetado em escala métrica real
            val leaderScreen = toScreen(leaderLocation)
            val mpp = metersPerPixelAtLat(leaderLocation.latitude, mapZoom, density)
            val radiusPx = ((maxSeparationDistanceKm * 1000.0) / mpp).toFloat().coerceIn(24f, w * 4f)
            drawCircle(
                color = FigmaTealPrimary.copy(alpha = 0.08f),
                radius = radiusPx,
                center = leaderScreen
            )
            drawCircle(
                color = FigmaTealPrimary.copy(alpha = 0.45f),
                radius = radiusPx,
                center = leaderScreen,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            // 5. Polilinha da Rota Viária Real (OSRM / GeoJSON) em Neon Ciano #38F0FC
            val routePath = Path().apply {
                val firstPt = toScreen(effectiveRoute.first())
                moveTo(firstPt.x, firstPt.y)
                effectiveRoute.drop(1).forEach { pt ->
                    val off = toScreen(pt)
                    lineTo(off.x, off.y)
                }
            }
            drawPath(
                path = routePath,
                color = FigmaNeonCyan.copy(alpha = 0.24f),
                style = Stroke(width = glowPulse, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = routePath,
                color = FigmaNeonCyan.copy(alpha = 0.50f),
                style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = routePath,
                color = FigmaNeonCyan,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Ponto discreto de Origem na rota
            val originScreen = toScreen(originCoordinates)
            drawCircle(
                color = FigmaTealPrimary,
                radius = 7f,
                center = originScreen
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = originScreen
            )

            // Marcador da Parada Intermediária na rota quando configurada
            if (intermediateStopCoordinates != null) {
                val stopScreen = toScreen(intermediateStopCoordinates)
                drawCircle(
                    color = FigmaOrangeLogo.copy(alpha = 0.30f),
                    radius = 16f,
                    center = stopScreen
                )
                drawCircle(
                    color = FigmaOrangeLogo,
                    radius = 7.5f,
                    center = stopScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = stopScreen
                )
            }

            // 6. Rota de desvio quando algum participante está afastado e ponto de reencontro
            val separatedPeer = participants.firstOrNull { it.status == ParticipantStatus.SEPARATED }
            if (isSimulatingSeparation || separatedPeer != null) {
                val sepPt = separatedPeer?.location
                val convergenceTarget = meetingPointCoordinates ?: destinationCoordinates
                if (sepPt != null) {
                    val sepScreen = toScreen(sepPt)
                    val targetScreen = toScreen(convergenceTarget)
                    val deviationPath = Path().apply {
                        moveTo(sepScreen.x, sepScreen.y)
                        val ctrlX = (sepScreen.x + targetScreen.x) * 0.5f
                        val ctrlY = minOf(sepScreen.y, targetScreen.y) - 48f
                        quadraticTo(ctrlX, ctrlY, targetScreen.x, targetScreen.y)
                    }
                    drawPath(
                        path = deviationPath,
                        color = FigmaOrangeLogo.copy(alpha = 0.88f),
                        style = Stroke(
                            width = 5.5f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 11f), 0f)
                        )
                    )
                }
            }

            if (meetingPointCoordinates != null) {
                val mpScreen = toScreen(meetingPointCoordinates)
                drawCircle(
                    color = FigmaTealPrimary.copy(alpha = 0.25f),
                    radius = 22f,
                    center = mpScreen
                )
                drawCircle(
                    color = FigmaTealPrimary,
                    radius = 9f,
                    center = mpScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = mpScreen
                )
            }
        }

        // Rótulo da Parada Intermediária no mapa quando configurada
        if (intermediateStopCoordinates != null && viewportSize.width > 0) {
            val stopOffset = projectToScreen(
                point = intermediateStopCoordinates,
                cameraLat = cameraLat,
                cameraLon = cameraLon,
                zoom = mapZoom,
                viewportWidthPx = viewportSize.width.toFloat(),
                viewportHeightPx = viewportSize.height.toFloat(),
                density = density
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (stopOffset.x - 14 * density).roundToInt(),
                            y = (stopOffset.y - 32 * density).roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F1209).copy(alpha = 0.92f))
                    .border(1.dp, FigmaOrangeLogo, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Parada: ${(intermediateStopName ?: "Intermediária").substringBefore(",")}",
                    color = FigmaOrangeLogo,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Pino geográfico do Ponto de Reencontro quando sugerido/confirmado
        if (meetingPointCoordinates != null && viewportSize.width > 0) {
            val mpOffset = projectToScreen(
                point = meetingPointCoordinates,
                cameraLat = cameraLat,
                cameraLon = cameraLon,
                zoom = mapZoom,
                viewportWidthPx = viewportSize.width.toFloat(),
                viewportHeightPx = viewportSize.height.toFloat(),
                density = density
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset {
                    IntOffset(
                        x = (mpOffset.x - 14 * density).roundToInt(),
                        y = (mpOffset.y - 34 * density).roundToInt()
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF062620).copy(alpha = 0.94f))
                        .border(1.dp, FigmaTealPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = selectedMeetingPointName ?: "Ponto de Reencontro",
                        color = FigmaTealPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pino geográfico do Destino Final
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            val destScreen = projectToScreen(
                point = destinationCoordinates,
                cameraLat = cameraLat,
                cameraLon = cameraLon,
                zoom = mapZoom,
                viewportWidthPx = viewportSize.width.toFloat(),
                viewportHeightPx = viewportSize.height.toFloat(),
                density = density
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset {
                    IntOffset(
                        x = (destScreen.x - 17 * density).roundToInt(),
                        y = (destScreen.y - 17 * density).roundToInt()
                    )
                }
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
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06152D).copy(alpha = 0.92f))
                        .border(1.dp, FigmaNeonCyan.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = destinationName.ifBlank { "Destino" },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Marcadores dos participantes ancorados nas coordenadas LatLng reais
            participants.forEachIndexed { index, p ->
                val pScreen = projectToScreen(
                    point = p.location,
                    cameraLat = cameraLat,
                    cameraLon = cameraLon,
                    zoom = mapZoom,
                    viewportWidthPx = viewportSize.width.toFloat(),
                    viewportHeightPx = viewportSize.height.toFloat(),
                    density = density
                )
                // Pequeno deslocamento caso dois participantes estejam exatamente na mesma coordenada (ex: no destino)
                val overlapOffsetPx = if (p.role == ParticipantRole.STATIONARY_HOST) index * 22 * density else 0f

                val isSeparated = p.status == ParticipantStatus.SEPARATED
                val isFocused = focusedParticipantId == p.id
                val ringColor = when {
                    isSeparated -> FigmaOrangeLogo
                    p.role == ParticipantRole.STATIONARY_HOST -> Color(0xFF8B5CF6)
                    else -> FigmaNeonCyan
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pScreen.x - 18 * density).roundToInt(),
                                y = (pScreen.y - 18 * density + overlapOffsetPx).roundToInt()
                            )
                        }
                        .clickable { onSelectParticipant(p.id) }
                ) {
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF06152D).copy(alpha = 0.92f))
                            .border(
                                if (isFocused) 1.5.dp else 1.dp,
                                ringColor.copy(alpha = if (isFocused) 1f else 0.8f),
                                RoundedCornerShape(8.dp)
                            )
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

        // Controles flutuantes do mapa (Zoom +, Zoom -, Enquadrar Rota, Meu GPS)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FigmaDarkBg.copy(alpha = 0.90f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapControlIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Aproximar zoom",
                onClick = { mapZoom = (mapZoom + 0.8f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM) }
            )
            HorizontalDivider(modifier = Modifier.width(24.dp), color = Color.White.copy(alpha = 0.12f))
            MapControlIconButton(
                icon = Icons.Default.Remove,
                contentDescription = "Afastar zoom",
                onClick = { mapZoom = (mapZoom - 0.8f).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM) }
            )
            HorizontalDivider(modifier = Modifier.width(24.dp), color = Color.White.copy(alpha = 0.12f))
            MapControlIconButton(
                icon = Icons.Default.CenterFocusStrong,
                contentDescription = "Enquadrar rota",
                onClick = { recenterToFullRoute() }
            )
            HorizontalDivider(modifier = Modifier.width(24.dp), color = Color.White.copy(alpha = 0.12f))
            MapControlIconButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Minha localização",
                tint = FigmaNeonCyan,
                onClick = {
                    val self = participants.firstOrNull { it.isCurrentUser }
                    if (self != null) {
                        cameraLat = self.location.latitude
                        cameraLon = self.location.longitude
                        mapZoom = 14.2f
                    }
                }
            )
        }
    }
}

@Composable
private fun MapControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}
