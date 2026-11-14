# Let's Drive

Aplicativo multiplataforma desenvolvido em **Kotlin Multiplatform (KMP)** e **Compose Multiplatform** para coordenação de viagens em grupo em tempo real, com acompanhamento de rota compartilhada, alertas geoespaciais de separação e definição colaborativa de pontos de reencontro.

---

## Stack Tecnológica e Arquitetura

### Núcleo Compartilhado (`commonMain`)
- **Linguagem e Framework Multiplataforma**: Kotlin Multiplatform (`2.2.20`) + Compose Multiplatform (`1.8.2`).
- **Rede e Serialização**: Ktor Client (`3.1.1`), `kotlinx-coroutines` (`1.10.1`) e `kotlinx-serialization-json` (`1.8.0`).
- **Backend e Banco Geoespacial (Supabase + PostGIS)**:
  - Cliente oficial `supabase-kt` (`3.1.1`) com módulos **Auth**, **Postgrest** e **Realtime**.
  - Banco **PostgreSQL + PostGIS** (`supabase/schema.sql`) utilizando colunas `geography(Point, 4326)`, índice espacial `GIST` (`idx_participant_locations_coords`), função RPC `check_trip_separation` (`ST_Distance` e `ST_DWithin`), histórico/status de viagens (`ACTIVE`, `COMPLETED`, `CANCELLED`), perfil persistente (`public.user_profiles`) e políticas Row Level Security (RLS).
- **Cartografia, Geocodificação e Roteamento**:
  - Projeção Web Mercator (`EPSG:3857`) implementada no canvas interativo (`InteractiveRoadMapCanvas`) com suporte a gestos de pan/pinch-zoom, área de raio métrico configurável e traçado vetorial da rota.
  - **OpenStreetMap Nominatim**: autocomplete de endereços e geocodificação direta/reversa (detecção automática do endereço de origem via GPS).
  - **OSRM Routing API**: cálculo de rota real multiponto em GeoJSON (origem, parada intermediária opcional e destino), distância total e ETA.
- **QR Code em Kotlin Puro**:
  - Codificador ISO/IEC 18004 Versão 2-L (`25x25`) com geração polinomial de correção de erro Reed-Solomon sobre $GF(2^8)$ (`QrCodeEncoder`), além de normalização e extração de códigos/links de convite.

### Integrações Nativas por Plataforma (`expect` / `actual`)
- **Android (`androidMain`)**:
  - Cliente HTTP Ktor `OkHttp`.
  - Rastreamento GPS via `LocationManager` (`PlatformLocationTracker.android.kt`).
  - Notificações locais de separação e ponto de encontro via `NotificationManagerCompat` / `POST_NOTIFICATIONS` (`PlatformNotifier.android.kt`).
  - Compartilhamento nativo (`Intent.ACTION_SEND`) e leitura de convite via `ClipboardManager` (`PlatformShareAndQr.android.kt`).
  - Persistência local via `SharedPreferences` (`PlatformStorage.android.kt`).
- **iOS (`iosMain` + `iosApp`)**:
  - Cliente HTTP Ktor `Darwin` e wrapper SwiftUI em `iosApp/iosApp/iOSApp.swift`.
  - Rastreamento via `CoreLocation` / `CLLocationManager` (`PlatformLocationTracker.ios.kt`).
  - Notificações locais via `UserNotifications` / `UNUserNotificationCenter` (`PlatformNotifier.ios.kt`).
  - Compartilhamento nativo via `UIActivityViewController` e leitura via `UIPasteboard` (`PlatformShareAndQr.ios.kt`).
  - Persistência local via `NSUserDefaults` (`PlatformStorage.ios.kt`).
- **Web (`wasmJsMain`)**:
  - Alvo WebAssembly (`wasmJs`) com integração a HTML5 Geolocation API, Web Notifications API, Web Share API (`navigator.share`), `localStorage` e leitura de parâmetros de convite via URL (`?join=`).

---

## Estrutura do Repositório

```text
lets-drive/
├── composeApp/
│   ├── build.gradle.kts                 # Configuração KMP (Android, iosX64/iosArm64/iosSimulatorArm64, wasmJs)
│   └── src/
│       ├── commonMain/kotlin/           # Lógica de domínio, Supabase SDK, OSRM/Nominatim, QR ISO, estado e telas
│       ├── androidMain/kotlin/          # Implementações Android e MainActivity
│       ├── iosMain/kotlin/              # Implementações iOS (CoreLocation, UNUserNotificationCenter, UIKit)
│       └── wasmJsMain/kotlin/           # Implementações WebAssembly
├── gradle/libs.versions.toml            # Catálogo de versões das dependências
├── iosApp/                              # Projeto Xcode wrapper (iOSApp.swift, Info.plist, Config.xcconfig)
├── scripts/serve.js                     # Servidor HTTP local para build Web Wasm
└── supabase/schema.sql                  # Schema PostgreSQL + PostGIS, RPC espacial, Realtime e políticas RLS
```

---

## Como Rodar o Aplicativo (Tutorial Completo)

### Pré-requisitos Gerais
1. **Git** instalado para clonar o repositório:
   ```bash
   git clone https://github.com/Vini1227/lets-drive.git
   cd lets-drive
   ```
2. **Java Development Kit (JDK 17 ou superior)** configurado na variável `JAVA_HOME` (exigido pelo Gradle `9.1.0` e pelo target `JVM_17`).

---

### Passo 1: Configurar o Backend (Supabase + PostGIS)
> O aplicativo também funciona com fallback local para testes básicos, mas para sincronização de grupo em tempo real, autenticação e histórico remoto configure seu projeto Supabase:

1. Crie um projeto gratuito no [Supabase](https://supabase.com/).
2. No painel do Supabase, acesse **SQL Editor**, cole todo o conteúdo de [`supabase/schema.sql`](supabase/schema.sql) e clique em **Run**.
   - Isso habilita a extensão `postgis`, cria as tabelas (`trips`, `participants`, `participant_locations`, `user_profiles`), a função `check_trip_separation`, o índice espacial `GIST`, o **Realtime** e as políticas **RLS**.
3. Acesse **Project Settings → API** no Supabase e copie a **Project URL** e a **anon public key**.
4. Ao abrir o app, acesse a aba **Configurações** e informe a **Project URL** e a **Anon Key** (ou defina-as em [`SupabaseConfig.kt`](composeApp/src/commonMain/kotlin/com/letsdrive/app/data/remote/SupabaseConfig.kt)).

---

### Passo 2: Como Rodar no Android

Requisitos específicos: **Android Studio** (recomendado: Ladybug ou mais recente) ou **Android SDK** com `compileSdk 36` (`minSdk 26` / Android 8.0+).

#### Opção A — Pelo Android Studio (Emulador ou Celular Físico)
1. Abra o Android Studio e clique em **Open**, selecionando a pasta raiz `lets-drive`.
2. Aguarde a sincronização do **Gradle** finalizar.
3. No seletor de dispositivos da barra superior:
   - Escolha um **Emulador Android** (AVD com API 26+); **ou**
   - Conecte seu celular Android via cabo USB com **Depuração USB** ativada nas *Opções do Desenvolvedor*.
4. Selecione a configuração de execução `composeApp` e clique em **Run (▶)**.
5. Ao abrir o app no aparelho, conceda as permissões de **Localização** e **Notificações**.

#### Opção B — Via Terminal (Gerar APK e Instalar com ADB)
- **Linux / macOS**:
  ```bash
  ./gradlew :composeApp:assembleDebug
  ```
- **Windows (PowerShell / CMD)**:
  ```powershell
  .\gradlew.bat :composeApp:assembleDebug
  ```

O APK será gerado em:
`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

Para instalar diretamente em um celular ou emulador conectado via ADB:
```bash
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

---

### Passo 3: Como Rodar no iOS (Simulador ou iPhone Físico)

> **Atenção**: Por restrição da Apple, a compilação e execução nativa para iOS exigem um **Mac (macOS)** com **Xcode** instalado.

#### Pré-requisitos no macOS
1. Instale o **Xcode** pela Mac App Store (e abra-o ao menos uma vez para aceitar os termos e instalar os componentes/simuladores do iOS 16.0+).
2. Garanta que o **JDK 17+** esteja instalado no Mac (necessário porque o Xcode executa o Gradle durante o build para compilar o framework KMP `ComposeApp`):
   ```bash
   java -version
   ```
   *(Se necessário, instale via Homebrew: `brew install openjdk@17`)*
3. Clone o repositório no Mac e dê permissão de execução ao Gradle Wrapper:
   ```bash
   git clone https://github.com/Vini1227/lets-drive.git
   cd lets-drive
   chmod +x gradlew
   ```

#### Rodando pelo Xcode
1. *(Opcional, recomendado na primeira vez)* Pré-carregue o Gradle Wrapper e as dependências no terminal antes de abrir o Xcode:
   ```bash
   ./gradlew help
   ```
2. *(Opcional, para iPhone físico)* Configure seu Team e Bundle ID em [`iosApp/Configuration/Config.xcconfig`](iosApp/Configuration/Config.xcconfig) (ou na aba **Signing & Capabilities** no Xcode):
   - Preencha `TEAM_ID=` com o ID da sua conta Apple Developer (pode ser uma conta gratuita Apple ID vinculada no Xcode em *Settings → Accounts*).
   - Se for instalar em um **iPhone físico**, altere `BUNDLE_ID=com.letsdrive.app` para um identificador único (ex.: `com.seuusuario.letsdrive`).
3. Abra o projeto iOS no Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
4. No navegador à esquerda do Xcode, confira o target **`iosApp` → Signing & Capabilities** e certifique-se de que **Automatically manage signing** está ativo com seu **Team** selecionado.
5. **Escolher o destino e executar**:
   - No topo do Xcode, selecione um **Simulador de iPhone** (iOS 16.0+) ou seu **iPhone conectado via cabo USB** (com *Modo de Desenvolvedor* habilitado em *Ajustes → Privacidade e Segurança* no iOS).
   - Clique em **Run (▶)** ou pressione `Cmd + R`.
   - O Xcode executará automaticamente a fase de build `Compile Kotlin Framework` (`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`), compilará o código Swift (`iosApp/iosApp/iOSApp.swift`) e abrirá o aplicativo.
6. **Se rodar no Simulador**, ative uma rota ou coordenada simulada no menu do Simulador (**Features → Location → City Run / Freeway Drive**) para testar o GPS em movimento.

---

### Passo 4: Como Rodar na Web (WebAssembly)

Para compilar a versão Web (`wasmJs`):
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution
```
Ou para build de produção servida localmente via Node.js:
```bash
./gradlew :composeApp:wasmJsBrowserProductionWebpack
node scripts/serve.js
```
Em seguida, acesse `http://localhost:8080` no navegador.
