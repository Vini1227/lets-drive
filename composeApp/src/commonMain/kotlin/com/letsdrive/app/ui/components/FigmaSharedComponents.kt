package com.letsdrive.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.generated.resources.Res
import com.letsdrive.app.generated.resources.ic_logo_ld
import com.letsdrive.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * Logo oficial Let's Drive ("Sem desvios.") usando o vetor original `LOGO LD` do Figma.
 */
@Composable
fun LetsDriveBrandLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_logo_ld),
                contentDescription = "Logo Let's Drive",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(width = 64.dp, height = 50.dp)
            )

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = "LET’S",
                    color = FigmaOrangeLogo,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    lineHeight = 27.sp
                )
                Text(
                    text = "DRIVE",
                    color = FigmaMintLogo,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    lineHeight = 27.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Sem desvios.",
            color = FigmaTextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 4.6.sp
        )
    }
}

/**
 * Topo com botão voltar opcional à esquerda e perfil ("Username / Fiat Uno") à direita.
 */
@Composable
fun FigmaTopProfileBar(
    userName: String,
    vehicleOrRoleLabel: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = FigmaTextWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onProfileClick() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = userName,
                    color = FigmaTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp
                )
                Text(
                    text = vehicleOrRoleLabel,
                    color = FigmaTextSubtle,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = "Perfil",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
