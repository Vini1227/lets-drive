package com.letsdrive.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letsdrive.app.generated.resources.Res
import com.letsdrive.app.generated.resources.ic_google_logo
import com.letsdrive.app.ui.components.LetsDriveBrandLogo
import com.letsdrive.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * Tela 1: iPhone 17 - 1 (Boas-vindas com Logo "LET'S DRIVE - Sem desvios.")
 */
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        LetsDriveBrandLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 200.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(268.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(58.dp))
                    .background(FigmaTealPrimary)
                    .clickable { onLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entrar na minha conta",
                    color = FigmaTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .width(268.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(58.dp))
                    .background(FigmaSecondaryButtonBg)
                    .clickable { onRegisterClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fazer meu cadastro",
                    color = FigmaTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Componente reutilizável para o botão oficial "Entrar com o Google" (fiel ao iPhone 17 - 2 e diretrizes Google).
 */
@Composable
private fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_google_logo),
                contentDescription = "Logo do Google",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = Color(0xFF1F1F1F),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Tela 2: iPhone 17 - 2 ("Entre na sua conta" + Usuário + Senha + Entrar + Google)
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onLoginWithEmail: (email: String, pass: String) -> Unit = { _, _ -> }
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = FigmaTextWhite)
            }
        }

        Spacer(Modifier.height(4.dp))

        LetsDriveBrandLogo()

        Spacer(Modifier.height(42.dp))

        Column(modifier = Modifier.widthIn(max = 296.dp).fillMaxWidth()) {
            Text(
                text = "Entre na sua conta",
                color = FigmaTextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaDangerRed.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = FigmaDangerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "E-mail ou Usuário",
                color = FigmaTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = {
                    Text(
                        "Digite Seu E-mail",
                        color = FigmaTextWhite.copy(alpha = 0.25f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Senha",
                color = FigmaTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        "Digite Sua Senha",
                        color = FigmaTextWhite.copy(alpha = 0.25f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Esqueci a senha",
                color = FigmaTextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(Modifier.height(24.dp))

            // Botão Entrar (#10CEAB, rounded 6px)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(FigmaTealPrimary)
                    .clickable(enabled = !isLoading) {
                        onLoginWithEmail(username, password)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Entrar",
                        color = FigmaTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Divisor "Ou"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = FigmaTextWhite.copy(alpha = 0.3f)
                )
                Text(
                    text = "  Ou  ",
                    color = FigmaTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = FigmaTextWhite.copy(alpha = 0.3f)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Botão oficial "Entrar com o Google" com logo vetorial nítido
            GoogleSignInButton(
                text = "Entrar com o Google",
                onClick = { onLoginSuccess(username.ifBlank { "Caio" }) }
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Não tem uma conta? Cadastre-se",
            color = FigmaTextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}

/**
 * Tela complementar de Cadastro seguindo o mesmo visual do Login (iPhone 17 - 2)
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: (name: String, vehicle: String) -> Unit,
    onBackToLogin: () -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onRegisterWithEmail: (name: String, email: String, vehicle: String, pass: String) -> Unit = { _, _, _, _ -> }
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBackToLogin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = FigmaTextWhite)
            }
        }

        LetsDriveBrandLogo()

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.widthIn(max = 296.dp).fillMaxWidth()) {
            Text(
                text = "Crie sua conta",
                color = FigmaTextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = FigmaDangerRed.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = FigmaDangerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Seu Nome / Apelido", color = FigmaTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text("Ex: Caio", color = FigmaTextWhite.copy(alpha = 0.25f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text("E-mail", color = FigmaTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text("seuemail@exemplo.com", color = FigmaTextWhite.copy(alpha = 0.25f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text("Veículo ou Status Padrão", color = FigmaTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = vehicle,
                onValueChange = { vehicle = it },
                placeholder = {
                    Text("Ex: Fiat Uno / Em Casa", color = FigmaTextWhite.copy(alpha = 0.25f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                },
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text("Senha", color = FigmaTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text("Crie Uma Senha (mínimo 6 dígitos)", color = FigmaTextWhite.copy(alpha = 0.25f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(7.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FigmaTealPrimary,
                    unfocusedBorderColor = FigmaTextWhite.copy(alpha = 0.5f),
                    focusedTextColor = FigmaTextWhite,
                    unfocusedTextColor = FigmaTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(FigmaTealPrimary)
                    .clickable(enabled = !isLoading) {
                        onRegisterWithEmail(name, email, vehicle, password)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Concluir Cadastro",
                        color = FigmaTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            GoogleSignInButton(
                text = "Cadastrar com o Google",
                onClick = { onRegisterSuccess(name.ifBlank { "Caio" }, vehicle.ifBlank { "Fiat Uno" }) }
            )
        }
    }
}
