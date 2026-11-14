package com.letsdrive.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.data.location.PlaceSuggestion
import com.letsdrive.app.data.location.RouteMetrics
import com.letsdrive.app.data.location.RouteSearchService
import com.letsdrive.app.data.share.InviteShareMessages
import com.letsdrive.app.data.share.PlatformShareAndQr
import com.letsdrive.app.data.share.QrCodeEncoder
import com.letsdrive.app.generated.resources.*
import com.letsdrive.app.model.LatLng
import com.letsdrive.app.model.ParticipantRole
import com.letsdrive.app.model.Trip
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.ui.components.FigmaTopProfileBar
import com.letsdrive.app.ui.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/**
 * Tela 3: iPhone 17 - 3 ("Eae Caio, pra onde vamos hoje?" + Barra de busca + Ilustração central + Menu inferior)
 */
@Composable
fun HomeScreen(
    userName: String,
    userHandle: String,
    userVehicle: String,
    onSearchDestinationClick: () -> Unit,
    onEnterCodeClick: () -> Unit,
    onOpenActiveMapClick: () -> Unit,
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
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FigmaTopProfileBar(
                userName = userHandle,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = false,
                onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
            )

            Spacer(Modifier.height(34.dp))

            // Saudação fiel ao iPhone 17 - 3
            Text(
                text = "Eae $userName, pra onde vamos hoje?",
                color = FigmaTextLightGray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(18.dp))

            // Barra de busca arredondada (327x41, rounded 50px, bg white 19% opacity)
            Row(
                modifier = Modifier
                    .width(332.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.19f))
                    .clickable { onSearchDestinationClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar destino",
                    tint = FigmaTextWhite,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Praia da Palma, Alagoas",
                    color = FigmaTextWhite.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(Modifier.height(28.dp))

            // Ilustração central estilo "order-ride/cuate" do Figma (Smartphone com mapa + personagem + carro)
            HomeIllustrationCard(
                onClick = onOpenActiveMapClick
            )

            Spacer(Modifier.height(20.dp))

            // Texto inferior clicável para ir à tela de inserir código (iPhone 17 - 8)
            Text(
                text = "Participe De Uma Viagem\nCompartilhada Inserindo O\nCódigo Gerado Por Seus Amigos",
                color = FigmaTextWhite.copy(alpha = 0.28f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .clickable { onEnterCodeClick() }
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Atalhos rápidos para testar qualquer fluxo
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                OutlinedButton(
                    onClick = onEnterCodeClick,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FigmaTealPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = FigmaTealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inserir Código", color = FigmaTealPrimary, fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenActiveMapClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaTealPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver Mapa Neon", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Utilitário para posicionar cada camada SVG do Figma com as coordenadas percentuais exatas (`inset-[top_right_bottom_left]`).
 */
@Composable
private fun FigmaInsetSvgLayer(
    painter: Painter,
    boxWidthDp: Float,
    boxHeightDp: Float,
    groupTop: Float,
    groupLeft: Float,
    groupWidth: Float,
    groupHeight: Float,
    top: Float,
    right: Float,
    bottom: Float,
    left: Float
) {
    val xDp = ((left - groupLeft) / groupWidth) * boxWidthDp
    val yDp = ((top - groupTop) / groupHeight) * boxHeightDp
    val wDp = (((100f - right) - left) / groupWidth) * boxWidthDp
    val hDp = (((100f - bottom) - top) / groupHeight) * boxHeightDp

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .size(width = wDp.dp, height = hDp.dp)
    )
}

/**
 * Ilustração oficial `order-ride/cuate` (node 44:1061 do frame iPhone 17 - 3 no Figma, 269.43 x 297.01)
 */
@Composable
private fun HomeIllustrationCard(onClick: () -> Unit) {
    val boxW = 269.43f
    val boxH = 297.01f
    val gTop = 33.07f
    val gLeft = 16.67f
    val gWidth = 67.02f  // 100 - 16.31 - 16.67
    val gHeight = 33.98f // 100 - 33.07 - 32.95

    Box(
        modifier = Modifier
            .size(width = boxW.dp, height = boxH.dp)
            .clickable { onClick() }
    ) {
        FigmaInsetSvgLayer(painterResource(Res.drawable.cuate_bg_complete), boxW, boxH, gTop, gLeft, gWidth, gHeight, 33.07f, 17.08f, 33.14f, 16.98f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.cuate_floor), boxW, boxH, gTop, gLeft, gWidth, gHeight, 66.86f, 16.61f, 33.07f, 16.98f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.cuate_plant), boxW, boxH, gTop, gLeft, gWidth, gHeight, 55.74f, 70.90f, 33.07f, 16.98f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.cuate_device), boxW, boxH, gTop, gLeft, gWidth, gHeight, 38.42f, 35.13f, 33.14f, 32.04f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.cuate_character), boxW, boxH, gTop, gLeft, gWidth, gHeight, 39.50f, 16.64f, 32.95f, 60.73f)
    }
}

/**
 * Tela 4: iPhone 17 - 8 ("Código da Viagem" + "Insira o Código da Viagem" + Ilustração + Modo Parado/Em Movimento)
 */
@Composable
fun JoinCodeScreen(
    userHandle: String,
    userVehicle: String,
    defaultRole: ParticipantRole,
    onJoinTrip: (code: String, role: ParticipantRole) -> Unit,
    onCreateNewRouteClick: () -> Unit,
    onNavigateMenu: (AppScreen) -> Unit
) {
    var tripCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(defaultRole) }
    var showInviteHelperDialog by remember { mutableStateOf(false) }
    var pastedInviteInput by remember { mutableStateOf("") }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

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
                userName = userHandle,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = true,
                onBackClick = { onNavigateMenu(AppScreen.HOME) },
                onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
            )

            Spacer(Modifier.height(18.dp))

            Column(modifier = Modifier.width(318.dp)) {
                Text(
                    text = "Código da Viagem",
                    color = FigmaTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tripCode,
                    onValueChange = { tripCode = it.uppercase() },
                    placeholder = {
                        Text(
                            text = "Insira O Código Da Viagem (Ex: #AB12CD34)",
                            color = FigmaTextWhite.copy(alpha = 0.25f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(7.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FigmaTealPrimary,
                        unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.6f),
                        focusedTextColor = FigmaTextWhite,
                        unfocusedTextColor = FigmaTextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Dificuldade Com O Código?",
                    color = FigmaTealPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showInviteHelperDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (scanFeedback != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = scanFeedback.orEmpty(),
                        color = FigmaNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Sugestão do professor: participar em movimento OU aguardando parado em casa
                Text(
                    text = "Como você vai participar?",
                    color = FigmaTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == ParticipantRole.MOVING_VEHICLE,
                        onClick = { selectedRole = ParticipantRole.MOVING_VEHICLE },
                        label = { Text("Em Movimento", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FigmaTealPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = FigmaTealPrimary,
                            selectedLeadingIconColor = FigmaTealPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedRole == ParticipantRole.STATIONARY_HOST,
                        onClick = { selectedRole = ParticipantRole.STATIONARY_HOST },
                        label = { Text("Aguardando no Local", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FigmaTealPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = FigmaTealPrimary,
                            selectedLeadingIconColor = FigmaTealPrimary
                        )
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Botão para entrar na viagem (completando a tela iPhone 17 - 8)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(FigmaTealPrimary)
                        .clickable { onJoinTrip(tripCode, selectedRole) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Entrar na Viagem Compartilhada",
                        color = FigmaTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(FigmaSheetBg)
                        .border(1.dp, FigmaTealPrimary.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
                        .clickable {
                            val detected = PlatformShareAndQr.scanOrReadInviteCode()
                                ?: QrCodeEncoder.extractInviteCode(clipboardManager.getText()?.text)
                            if (detected != null) {
                                tripCode = detected
                                scanFeedback = "Código $detected detectado do convite!"
                            } else {
                                pastedInviteInput = clipboardManager.getText()?.text.orEmpty()
                                showInviteHelperDialog = true
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = FigmaTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ler QR Code / Colar Convite",
                        color = FigmaTealPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Ilustração central isométrica (order-ride/amico do frame 44:152)
            IsometricRideIllustration()

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Participe De Uma Viagem\nCompartilhada Inserindo O\nCódigo Gerado Por Seus Amigos",
                color = FigmaTextWhite.copy(alpha = 0.22f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onCreateNewRouteClick) {
                Text(
                    text = "Ou toque aqui para criar uma nova viagem",
                    color = FigmaTealPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showInviteHelperDialog) {
            val extractedPreview = QrCodeEncoder.extractInviteCode(pastedInviteInput)
            AlertDialog(
                onDismissRequest = { showInviteHelperDialog = false },
                containerColor = FigmaSheetBg,
                title = {
                    Text(
                        text = "Ler QR Code ou Colar Convite",
                        color = FigmaTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Aponte a câmera nativa do celular para o QR Code do anfitrião ou cole abaixo o texto/link recebido no WhatsApp:",
                            color = FigmaTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pastedInviteInput,
                            onValueChange = { pastedInviteInput = it },
                            placeholder = {
                                Text("Ex: https://letsdrive.app/#AB123456 ou #AB123456", fontSize = 11.sp)
                            },
                            maxLines = 3,
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (extractedPreview != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Código extraído: $extractedPreview",
                                color = FigmaNeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val codeToUse = extractedPreview ?: pastedInviteInput.trim()
                            if (codeToUse.isNotBlank()) {
                                tripCode = codeToUse
                                scanFeedback = "Convite aplicado: $codeToUse"
                                showInviteHelperDialog = false
                                onJoinTrip(codeToUse, selectedRole)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaTealPrimary)
                    ) {
                        Text("Usar Código e Entrar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInviteHelperDialog = false }) {
                        Text("Fechar", color = FigmaTextMuted)
                    }
                }
            )
        }
    }
}

/**
 * Ilustração oficial `order-ride/amico` (node 44:157 do frame iPhone 17 - 8 no Figma, 284.66 x 280.38)
 */
@Composable
private fun IsometricRideIllustration() {
    val boxW = 284.66f
    val boxH = 280.38f
    val gTop = 35.47f
    val gLeft = 14.68f
    val gWidth = 70.81f  // 100 - 14.51 - 14.68
    val gHeight = 32.08f // 100 - 35.47 - 32.45

    Box(modifier = Modifier.size(width = boxW.dp, height = boxH.dp)) {
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_building), boxW, boxH, gTop, gLeft, gWidth, gHeight, 42.81f, 17.46f, 44.76f, 17.63f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_floor), boxW, boxH, gTop, gLeft, gWidth, gHeight, 48.74f, 14.51f, 32.45f, 14.68f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_shadow), boxW, boxH, gTop, gLeft, gWidth, gHeight, 51.85f, 22.40f, 45.42f, 67.52f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_tree), boxW, boxH, gTop, gLeft, gWidth, gHeight, 44.41f, 74.36f, 47.35f, 16.72f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_bush), boxW, boxH, gTop, gLeft, gWidth, gHeight, 52.02f, 77.75f, 47.00f, 19.68f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_city_lights), boxW, boxH, gTop, gLeft, gWidth, gHeight, 40.59f, 77.44f, 44.82f, 17.54f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_clouds), boxW, boxH, gTop, gLeft, gWidth, gHeight, 36.05f, 36.68f, 60.73f, 35.79f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_sun), boxW, boxH, gTop, gLeft, gWidth, gHeight, 35.47f, 72.89f, 61.67f, 20.90f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_char_bottom), boxW, boxH, gTop, gLeft, gWidth, gHeight, 46.57f, 23.94f, 46.38f, 69.05f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_char_top), boxW, boxH, gTop, gLeft, gWidth, gHeight, 43.33f, 23.09f, 52.42f, 67.36f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_mobile), boxW, boxH, gTop, gLeft, gWidth, gHeight, 47.28f, 15.51f, 35.26f, 24.82f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_map), boxW, boxH, gTop, gLeft, gWidth, gHeight, 48.25f, 32.22f, 41.38f, 28.61f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_options), boxW, boxH, gTop, gLeft, gWidth, gHeight, 54.32f, 26.34f, 39.86f, 51.79f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_button), boxW, boxH, gTop, gLeft, gWidth, gHeight, 56.46f, 22.62f, 38.76f, 61.62f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_pin), boxW, boxH, gTop, gLeft, gWidth, gHeight, 41.05f, 61.71f, 52.18f, 28.84f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_road), boxW, boxH, gTop, gLeft, gWidth, gHeight, 57.19f, 37.75f, 33.58f, 15.08f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_car_shadow), boxW, boxH, gTop, gLeft, gWidth, gHeight, 57.61f, 59.90f, 36.04f, 16.40f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_car_interiors), boxW, boxH, gTop, gLeft, gWidth, gHeight, 56.03f, 65.06f, 38.91f, 17.78f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_car_back), boxW, boxH, gTop, gLeft, gWidth, gHeight, 56.17f, 67.51f, 38.04f, 16.32f)
        FigmaInsetSvgLayer(painterResource(Res.drawable.amico_car_front), boxW, boxH, gTop, gLeft, gWidth, gHeight, 57.20f, 60.84f, 36.67f, 25.73f)
    }
}

/**
 * Tela 5: iPhone 17 - 4 (Definir Origem + Destino + Limite Máximo de Distância + Botão "Confirmar Destino")
 */
@Composable
fun ConfirmRouteScreen(
    userHandle: String,
    userVehicle: String,
    initialOrigin: String,
    initialDestination: String,
    initialMaxDistanceKm: Double,
    initialRole: ParticipantRole,
    initialExtraStop: String? = null,
    currentUserLocation: LatLng = LatLng(-9.6498, -35.7089),
    onConfirmRoute: (origin: String, destination: String, extraStop: String?, maxDistKm: Double, role: ParticipantRole) -> Unit,
    onBack: () -> Unit,
    onNavigateMenu: (AppScreen) -> Unit
) {
    var origin by remember { mutableStateOf(initialOrigin) }
    var destination by remember { mutableStateOf(initialDestination) }
    var extraStop by remember { mutableStateOf(initialExtraStop.orEmpty()) }
    var showExtraStop by remember { mutableStateOf(!initialExtraStop.isNullOrBlank()) }
    var activeSearchTarget by remember { mutableStateOf("dest") }
    var isResolvingGpsOrigin by remember { mutableStateOf(false) }
    var maxDistanceKm by remember { mutableStateOf(initialMaxDistanceKm.toFloat()) }
    var selectedRole by remember { mutableStateOf(initialRole) }

    val coroutineScope = rememberCoroutineScope()
    val routeSearchService = remember { RouteSearchService() }
    var placeSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var routeMetrics by remember { mutableStateOf<RouteMetrics?>(null) }

    // Busca sugestões (OpenStreetMap Nominatim + catálogo) e calcula rota OSRM (com parada opcional)
    LaunchedEffect(destination, origin, extraStop, showExtraStop, activeSearchTarget) {
        val activeQuery = if (activeSearchTarget == "stop" && showExtraStop) extraStop else destination
        placeSuggestions = routeSearchService.searchPlaces(activeQuery)
        routeMetrics = routeSearchService.calculateRoute(
            originText = origin,
            destinationText = destination,
            extraStopText = if (showExtraStop) extraStop else null
        )
    }

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
                userName = userHandle,
                vehicleOrRoleLabel = userVehicle,
                showBackButton = true,
                onBackClick = onBack,
                onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
            )

            Spacer(Modifier.height(28.dp))

            // Bloco de Origem e Destino conectados por linha lateral + botão "+" (exatamente como no iPhone 17 - 4)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Conector lateral esquerdo (Vector 4 do Figma)
                Canvas(modifier = Modifier.size(16.dp, 72.dp)) {
                    val w = size.width
                    val h = size.height
                    drawCircle(color = Color.White, radius = 4f, center = Offset(w * 0.8f, h * 0.15f))
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.8f, h * 0.15f),
                        end = Offset(w * 0.2f, h * 0.15f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.2f, h * 0.15f),
                        end = Offset(w * 0.2f, h * 0.85f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.2f, h * 0.85f),
                        end = Offset(w * 0.8f, h * 0.85f),
                        strokeWidth = 2f
                    )
                    drawCircle(color = Color.White, radius = 4f, center = Offset(w * 0.8f, h * 0.85f))
                }

                Spacer(Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (!isResolvingGpsOrigin) {
                                        isResolvingGpsOrigin = true
                                        coroutineScope.launch {
                                            origin = routeSearchService.reverseGeocode(currentUserLocation)
                                            isResolvingGpsOrigin = false
                                        }
                                    }
                                }
                            ) {
                                if (isResolvingGpsOrigin) {
                                    CircularProgressIndicator(
                                        color = FigmaTealPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Usar localização GPS atual",
                                        tint = FigmaTealPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(7.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FigmaTealPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {
                                destination = it
                                activeSearchTarget = "dest"
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(7.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FigmaTealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Botão "+" circular na borda direita do campo de destino (Ellipse 21 do Figma)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD9D9D9))
                                .clickable {
                                    showExtraStop = !showExtraStop
                                    activeSearchTarget = if (showExtraStop) "stop" else "dest"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (showExtraStop) "-" else "+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (showExtraStop) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = extraStop,
                            onValueChange = {
                                extraStop = it
                                activeSearchTarget = "stop"
                            },
                            placeholder = {
                                Text(
                                    "Parada intermediária opcional",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = FigmaTealPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(7.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FigmaTealPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.55f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Sugestões Rápidas de Endereço (OpenStreetMap Nominatim + Destinos Populares)
            if (placeSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    placeSuggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                if (activeSearchTarget == "stop" && showExtraStop) {
                                    extraStop = suggestion.fullAddress
                                } else {
                                    destination = suggestion.fullAddress
                                }
                            },
                            label = {
                                Text(
                                    text = suggestion.title,
                                    color = FigmaTextWhite,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = FigmaTealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = FigmaSheetBg
                            )
                        )
                    }
                }
            }

            // Resumo da Rota Calculada (OSRM Routing Engine / Geodésia)
            routeMetrics?.let { metrics ->
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FigmaTealPrimary.copy(alpha = 0.14f))
                        .border(1.dp, FigmaTealPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = FigmaTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = metrics.mainHighwayName,
                            color = FigmaTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${metrics.distanceKm} km • ~${metrics.durationMinutes} min",
                        color = FigmaNeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sugestão do Professor: Configurar Distância Máxima antes do Pop-up de Alerta + Modo Parado/Em Movimento
            Card(
                colors = CardDefaults.cardColors(containerColor = FigmaSheetBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alerta de Distância Máxima",
                            color = FigmaTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(maxDistanceKm * 10).roundToInt() / 10.0} km",
                            color = FigmaNeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "Notifica o grupo se algum participante se afastar além desse raio.",
                        color = FigmaTextMuted,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = maxDistanceKm,
                        onValueChange = { maxDistanceKm = it },
                        valueRange = 0.5f..10.0f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = FigmaTealPrimary,
                            activeTrackColor = FigmaTealPrimary
                        )
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Seu status nesta viagem:",
                        color = FigmaTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRole == ParticipantRole.MOVING_VEHICLE,
                            onClick = { selectedRole = ParticipantRole.MOVING_VEHICLE },
                            label = { Text("Indo até lá", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedRole == ParticipantRole.STATIONARY_HOST,
                            onClick = { selectedRole = ParticipantRole.STATIONARY_HOST },
                            label = { Text("Esperando no local", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            // Botão "Confirmar Destino" (#10CEAB, 318x46, rounded 6.5px) igual ao iPhone 17 - 4
            Box(
                modifier = Modifier
                    .width(318.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(FigmaTealPrimary)
                    .clickable {
                        val roundedDist = (maxDistanceKm * 10).roundToInt() / 10.0
                        val activeStop = extraStop.trim().takeIf { showExtraStop && it.isNotBlank() }
                        onConfirmRoute(origin, destination, activeStop, roundedDist, selectedRole)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Confirmar Destino",
                    color = FigmaTextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Tela 6: iPhone 17 - 6 (Bottom Sheet "A viagem foi iniciada para Praia da Palma, Alagoas" + Código #AB12CD34 + QR Code)
 */
@Composable
fun RoomCreatedScreen(
    trip: Trip,
    userHandle: String,
    userVehicle: String,
    onOpenMapClick: () -> Unit,
    onShareInviteClick: () -> Unit = {},
    onCancelTripClick: () -> Unit,
    onNavigateMenu: (AppScreen) -> Unit
) {
    var copiedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        // Topo com Perfil
        FigmaTopProfileBar(
            userName = userHandle,
            vehicleOrRoleLabel = userVehicle,
            showBackButton = true,
            onBackClick = onCancelTripClick,
            onProfileClick = { onNavigateMenu(AppScreen.PROFILE) }
        )

        // Bottom Sheet #242424 com cantos superiores 50px (exatamente como Rectangle 17 no iPhone 17 - 6)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                .background(FigmaSheetBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 16.dp)
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alça superior (Line 12 - 53px)
            Box(
                modifier = Modifier
                    .width(53.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.7f))
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "A viagem foi iniciada para",
                color = FigmaTextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = trip.destinationName,
                color = FigmaTextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Código Da Sala Gerado",
                color = FigmaTextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(trip.inviteCode))
                        copiedFeedback = true
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar código",
                    tint = FigmaTextWhite,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = trip.inviteCode,
                    color = FigmaTextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (copiedFeedback) {
                Text(
                    text = "Código ${trip.inviteCode} copiado para a área de transferência!",
                    color = FigmaTealPrimary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // QR Code dinâmico de 195x195 gerado para o código específico da sala (iPhone 17 - 6)
            QrCodeGraphic(
                inviteCode = trip.inviteCode,
                modifier = Modifier.size(195.dp)
            )

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FigmaDarkBg.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Participantes na sala (${trip.participants.size})",
                    color = FigmaNeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                trip.participants.forEach { participant ->
                    Text(
                        text = "• ${participant.displayName} (${participant.vehicleLabel})",
                        color = FigmaTextWhite,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                if (trip.participants.size == 1) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Compartilhe o código ${trip.inviteCode} para outros participantes entrarem.",
                        color = FigmaTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Botão Principal para Abrir o Mapa da Viagem (iPhone 17 - 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(FigmaTealPrimary)
                    .clickable { onOpenMapClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Abrir Mapa em Tempo Real",
                    color = FigmaTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            // Botão Nativo de Share Sheet + Cópia de Convite Completo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(FigmaDarkBg)
                    .border(1.dp, FigmaTealPrimary, RoundedCornerShape(7.dp))
                    .clickable {
                        val inviteMsg = InviteShareMessages.buildInviteText(
                            inviteCode = trip.inviteCode,
                            destinationName = trip.destinationName,
                            inviteLink = trip.inviteLink
                        )
                        clipboardManager.setText(AnnotatedString(inviteMsg))
                        copiedFeedback = true
                        onShareInviteClick()
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = FigmaTealPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Compartilhar Convite (WhatsApp / Sistema)",
                    color = FigmaTealPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            // Botão secundário "Cancelar Viagem" do frame 1:445
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(FigmaSecondaryButtonBg)
                    .clickable { onCancelTripClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancelar Viagem",
                    color = FigmaTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QrCodeGraphic(
    inviteCode: String,
    modifier: Modifier = Modifier
) {
    val payload = remember(inviteCode) { QrCodeEncoder.buildQrPayload(inviteCode) }
    val matrix = remember(payload) { QrCodeEncoder.encodeVersion2EccL(payload) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = matrix.size
            val cellSize = size.width / grid.toFloat()
            val bleed = 0.5f

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + bleed, cellSize + bleed)
                        )
                    }
                }
            }
        }
    }
}
