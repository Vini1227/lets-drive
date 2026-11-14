package com.letsdrive.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.state.AppScreen
import com.letsdrive.app.ui.theme.FigmaDarkBg
import kotlinx.coroutines.launch
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.abs
import kotlin.math.roundToInt

private data class IosGlassTab(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isPlusAction: Boolean = false
)

private val IosLiquidTabs = listOf(
    IosGlassTab(AppScreen.HOME, "Início", Icons.Filled.Home, Icons.Outlined.Home),
    IosGlassTab(AppScreen.HISTORY, "Histórico", Icons.Filled.History, Icons.Outlined.History),
    IosGlassTab(AppScreen.JOIN_CODE, "Viagem", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, isPlusAction = true),
    IosGlassTab(AppScreen.SETTINGS, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
    IosGlassTab(AppScreen.PROFILE, "Você", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

private fun resolveTabIndex(currentScreen: AppScreen): Int = when (currentScreen) {
    AppScreen.HOME, AppScreen.SHARED_MAP -> 0
    AppScreen.HISTORY -> 1
    AppScreen.JOIN_CODE, AppScreen.CONFIRM_ROUTE, AppScreen.ROOM_CREATED -> 2
    AppScreen.SETTINGS -> 3
    AppScreen.PROFILE -> 4
    else -> 0
}

/**
 * Shader GPU SkSL reproduzindo a óptica física do Apple iOS 26 Liquid Glass (WhatsApp & Tinder):
 * - Signed Distance Field (SDF) de cápsula arredondada
 * - Lupa central + Distorção de lente convexa 3D pela Lei de Snell nas bordas curvas
 * - Dispersão cromática prismática (separação RGB + refração verde/ciano/azul nas bordas como na Foto 2 do WhatsApp)
 * - Reflexo especular de vidro curvo nas bordas
 */
private const val APPLE_LIQUID_GLASS_SKSL = """
uniform shader content;
uniform float2 resolution;
uniform float2 lensCenter;
uniform float2 lensHalfSize;
uniform float cornerRadius;
uniform float activation;
uniform float velocityX;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 fragCoord) {
    if (activation <= 0.005) {
        return half4(0.0);
    }

    float2 p = fragCoord - lensCenter;
    float d = sdRoundedBox(p, lensHalfSize, cornerRadius);

    // Fora da gota de vidro: totalmente transparente
    if (d > 1.0) {
        return half4(0.0);
    }

    // Borda externa com anti-aliasing suave
    float edgeAlpha = clamp(1.0 - d, 0.0, 1.0) * clamp(activation * 1.4, 0.0, 1.0);

    // Distância normalizada da borda (0.0 na borda externa -> 1.0 no centro da lente)
    float minDim = min(lensHalfSize.x, lensHalfSize.y);
    float bezelWidth = minDim * 0.52;
    float edgeDist = clamp(-d / max(bezelWidth, 1.0), 0.0, 1.0);

    // Vetor Normal 2D da superfície curva da gota apontando para fora
    float2 eps = float2(1.5, 0.0);
    float dx = sdRoundedBox(p + eps.xy, lensHalfSize, cornerRadius) - sdRoundedBox(p - eps.xy, lensHalfSize, cornerRadius);
    float dy = sdRoundedBox(p + eps.yx, lensHalfSize, cornerRadius) - sdRoundedBox(p - eps.yx, lensHalfSize, cornerRadius);
    float2 normal2D = normalize(float2(dx, dy) + 0.0001);

    // Perfil de curvatura 3D da lente líquida nas bordas
    float rimCurve = pow(1.0 - edgeDist, 2.2);

    // Lupa óptica no centro (amplia ~18% o ícone e texto sob a gota) + Refração de Snell forte na borda
    float zoomFactor = mix(1.0, 0.83, activation);
    float2 zoomedP = p * zoomFactor;
    float2 refractOffset = -normal2D * rimCurve * 24.0 * activation;

    float2 sampleBase = lensCenter + zoomedP + refractOffset;

    // Aberração Cromática (separação física dos canais R, G, B nas curvas da lente)
    float dispersion = rimCurve * 7.0 * activation;
    float2 rCoord = clamp(sampleBase + normal2D * dispersion * 1.2, float2(1.0), resolution - 1.0);
    float2 gCoord = clamp(sampleBase - normal2D * dispersion * 0.2, float2(1.0), resolution - 1.0);
    float2 bCoord = clamp(sampleBase - normal2D * dispersion * 1.3, float2(1.0), resolution - 1.0);

    half4 colR = content.eval(rCoord);
    half4 colG = content.eval(gCoord);
    half4 colB = content.eval(bCoord);

    half3 refracted = half3(colR.r, colG.g, colB.b);

    // Mantém os ícones brancos 100% nítidos e brilhantes enquanto dá profundidade cristalina escura ao fundo da gota
    float luma = dot(refracted, half3(0.299, 0.587, 0.114));
    half3 deepGlassBg = refracted * half3(0.55, 0.58, 0.62);
    half3 crispContent = mix(deepGlassBg, refracted * 1.08, smoothstep(0.15, 0.65, luma));

    // Dispersão Prismática do iOS 26 nas bordas curvas (verde/esmeralda no topo-direito, azul/ciano/ âmbar na base, igual à Foto 2 do WhatsApp)
    float topMask = clamp(dot(normal2D, normalize(float2(0.35, -0.92))), 0.0, 1.0);
    float bottomMask = clamp(dot(normal2D, normalize(float2(-0.35, 0.92))), 0.0, 1.0);
    float leftMask = clamp(dot(normal2D, normalize(float2(-0.85, -0.45))), 0.0, 1.0);

    float prismRing = pow(1.0 - edgeDist, 2.8) * activation;
    half3 emeraldRefraction = half3(0.15, 0.85, 0.42) * pow(topMask, 2.0) * prismRing * 0.72;
    half3 cyanWarmRefraction = half3(0.28, 0.68, 0.98) * pow(bottomMask, 2.2) * prismRing * 0.55;
    half3 violetRefraction = half3(0.65, 0.45, 0.95) * pow(leftMask, 2.5) * prismRing * 0.38;

    // Anel Especular de Vidro (Reflexo branco brilhante fino contornando a cápsula)
    float tilt = clamp(velocityX * 0.0004, -0.35, 0.35);
    float2 lightDirA = normalize(float2(-0.55 + tilt, -0.82));
    float2 lightDirB = normalize(float2(0.55 + tilt, 0.82));
    float specA = pow(clamp(dot(normal2D, lightDirA), 0.0, 1.0), 5.0);
    float specB = pow(clamp(dot(normal2D, lightDirB), 0.0, 1.0), 6.0);

    // Borda fina nítida de cristal na extremidade da gota
    float outerBorder = smoothstep(2.2, 0.0, abs(d + 1.0));
    float bevelGlow = pow(1.0 - edgeDist, 4.2);

    float rimHighlight = (outerBorder * 0.78 + (specA * 0.9 + specB * 0.65) * bevelGlow) * activation;

    half3 finalRgb = crispContent + emeraldRefraction + cyanWarmRefraction + violetRefraction + half3(rimHighlight);

    return half4(clamp(finalRgb, half3(0.0), half3(1.0)), half(edgeAlpha));
}
"""

@Composable
actual fun MainNavigationShell(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onPlusButtonClick: () -> Unit,
    modifier: Modifier,
    screenContent: @Composable (AppScreen) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val backdropLayer = rememberGraphicsLayer()
    val liquidRuntimeEffect = remember { RuntimeEffect.makeForShader(APPLE_LIQUID_GLASS_SKSL) }

    val committedTabIndex = resolveTabIndex(currentScreen)
    val tabCount = IosLiquidTabs.size

    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var barRootOffset by remember { mutableStateOf(Offset.Zero) }

    // false = Estado 1 (Parado, cápsula cinza fosca igual Foto 1 do WhatsApp)
    // true  = Estado 2 (Tocando/Arrastando com o dedo, bolha Liquid Glass inflada com refração igual Foto 2 do WhatsApp)
    var isDraggingOrPressed by remember { mutableStateOf(false) }

    // Posição contínua (0f .. 4f) da cápsula/gota
    val pillIndexAnim = remember { Animatable(committedTabIndex.toFloat()) }
    var dragVelocityX by remember { mutableStateOf(0f) }

    LaunchedEffect(committedTabIndex) {
        if (!isDraggingOrPressed) {
            pillIndexAnim.animateTo(
                targetValue = committedTabIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val glassActivation by animateFloatAsState(
        targetValue = if (isDraggingOrPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.66f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassActivation"
    )

    val stretchFactor by animateFloatAsState(
        targetValue = if (isDraggingOrPressed) (1f + (abs(dragVelocityX) / 2600f).coerceIn(0f, 0.20f)) else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium),
        label = "JellyStretch"
    )

    val latestOnSelect by rememberUpdatedState(onSelectScreen)
    val latestOnPlus by rememberUpdatedState(onPlusButtonClick)
    val latestCommittedIdx by rememberUpdatedState(committedTabIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
    ) {
        // 1. Tudo aqui dentro é gravado no backdropLayer em tempo real para que a lente Liquid Glass refrate tanto o fundo quanto os ícones!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdropLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(backdropLayer)
                }
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 160),
                label = "ScreenCrossfade"
            ) { targetScreen ->
                screenContent(targetScreen)
            }

            // Pílula Flutuante estilo iOS 26 (WhatsApp / Tinder)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .onGloballyPositioned { coords ->
                        barSize = coords.size
                        barRootOffset = coords.positionInRoot()
                    }
                    .drawWithContent {
                        val barCorner = CornerRadius(size.height / 2f, size.height / 2f)
                        // Fundo escuro translúcido da pílula principal (idêntico à Foto 1 do WhatsApp)
                        drawRoundRect(
                            color = Color(0xDF121214),
                            cornerRadius = barCorner
                        )
                        // Borda fina sutil da pílula principal
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            cornerRadius = barCorner,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Cápsula cinza fosca do Estado 1 (Parado - Foto 1 do WhatsApp)
                        // Desaparece suavemente quando a bolha Liquid Glass (Estado 2) entra em ação!
                        val restingAlpha = (1f - glassActivation).coerceIn(0f, 1f)
                        if (restingAlpha > 0.01f && size.width > 0f) {
                            val padPx = 5.dp.toPx()
                            val innerW = size.width - padPx * 2f
                            val slotW = innerW / tabCount.toFloat()
                            val pillW = slotW * 0.96f
                            val pillH = 54.dp.toPx()
                            val centerX = padPx + (pillIndexAnim.value.coerceIn(0f, (tabCount - 1).toFloat()) + 0.5f) * slotW
                            val topLeftX = centerX - pillW / 2f
                            val topLeftY = (size.height - pillH) / 2f

                            drawRoundRect(
                                color = Color(0xFF2C2C30).copy(alpha = restingAlpha),
                                topLeft = Offset(topLeftX, topLeftY),
                                size = Size(pillW, pillH),
                                cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                            )
                        }

                        drawContent()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeVisualIdx = pillIndexAnim.value.roundToInt().coerceIn(0, tabCount - 1)
                    IosLiquidTabs.forEachIndexed { idx, tab ->
                        val isHighlighted = idx == activeVisualIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isHighlighted) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = if (isHighlighted) Color.White else Color(0xFFA0A0A8),
                                    modifier = Modifier.size(23.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = tab.label,
                                    color = if (isHighlighted) Color.White else Color(0xFFA0A0A8),
                                    fontSize = 10.sp,
                                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Lente / Gota 3D Liquid Glass (Estado 2 - Foto 2 do WhatsApp) + Gesto Contínuo com o Dedo
        if (barSize.width > 0 && barSize.height > 0) {
            val barWidthPx = barSize.width.toFloat()
            val barHeightPx = barSize.height.toFloat()
            val horizontalPadPx = with(density) { 5.dp.toPx() }
            val innerWidthPx = (barWidthPx - horizontalPadPx * 2f).coerceAtLeast(1f)
            val slotWidthPx = innerWidthPx / tabCount.toFloat()

            // Em repouso: 54dp (dentro da barra de 64dp)
            // Em movimento (Dedo na barra): infla para 80dp (ultrapassando a barra para cima e para baixo igual à Foto 2 do WhatsApp!)
            val restingPillHeightPx = with(density) { 54.dp.toPx() }
            val activePillHeightPx = with(density) { 80.dp.toPx() }
            val currentPillHeightPx = restingPillHeightPx + (activePillHeightPx - restingPillHeightPx) * glassActivation

            val restingPillWidthPx = slotWidthPx * 0.96f
            val activePillWidthPx = (slotWidthPx * 1.16f) * stretchFactor
            val currentPillWidthPx = restingPillWidthPx + (activePillWidthPx - restingPillWidthPx) * glassActivation

            val centerIndex = pillIndexAnim.value.coerceIn(0f, (tabCount - 1).toFloat())
            val lensCenterInBarX = horizontalPadPx + (centerIndex + 0.5f) * slotWidthPx
            val lensCenterInBarY = barHeightPx / 2f

            val lensCenterRootX = barRootOffset.x + lensCenterInBarX
            val lensCenterRootY = barRootOffset.y + lensCenterInBarY

            if (glassActivation > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val builder = RuntimeShaderBuilder(liquidRuntimeEffect).apply {
                                uniform("resolution", size.width, size.height)
                                uniform("lensCenter", lensCenterRootX, lensCenterRootY)
                                uniform("lensHalfSize", currentPillWidthPx / 2f, currentPillHeightPx / 2f)
                                uniform("cornerRadius", currentPillHeightPx / 2f)
                                uniform("activation", glassActivation)
                                uniform("velocityX", dragVelocityX)
                            }
                            val shaderFilter = ImageFilter.makeRuntimeShader(
                                builder,
                                shaderName = "content",
                                input = null
                            )
                            renderEffect = shaderFilter.asComposeRenderEffect()
                        }
                        .drawWithContent {
                            drawLayer(backdropLayer)
                        }
                )
            }

            // Área de Toque e Arraste Contínuo (Segurar e deslizar o dedo ativa a gota Liquid Glass; soltar confirma a aba)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(80.dp)
                    .pointerInput(tabCount, innerWidthPx, horizontalPadPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            isDraggingOrPressed = true
                            dragVelocityX = 0f

                            fun xToFractionalTab(xPx: Float): Float {
                                val relX = (xPx - horizontalPadPx).coerceIn(0f, innerWidthPx)
                                return ((relX / slotWidthPx) - 0.5f).coerceIn(0f, (tabCount - 1).toFloat())
                            }

                            val initialTarget = xToFractionalTab(down.position.x)
                            scope.launch {
                                pillIndexAnim.animateTo(
                                    targetValue = initialTarget,
                                    animationSpec = spring(
                                        dampingRatio = 0.76f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }

                            var currentTarget = initialTarget
                            var pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break

                                pointerId = change.id

                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }

                                val dx = change.positionChange().x
                                dragVelocityX = dx * 60f
                                currentTarget = xToFractionalTab(change.position.x)
                                change.consume()

                                scope.launch {
                                    pillIndexAnim.snapTo(currentTarget)
                                }
                            }

                            val finalTabIndex = currentTarget.roundToInt().coerceIn(0, tabCount - 1)
                            dragVelocityX = 0f
                            isDraggingOrPressed = false

                            scope.launch {
                                pillIndexAnim.animateTo(
                                    targetValue = finalTabIndex.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }

                            val chosenTab = IosLiquidTabs[finalTabIndex]
                            if (chosenTab.isPlusAction) {
                                if (latestCommittedIdx != 2) {
                                    latestOnSelect(AppScreen.JOIN_CODE)
                                } else {
                                    latestOnPlus()
                                }
                            } else {
                                latestOnSelect(chosenTab.screen)
                            }
                        }
                    }
            )
        }
    }
}
