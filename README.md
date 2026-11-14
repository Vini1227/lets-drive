# Let's Drive

Aplicativo multiplataforma (**Kotlin Multiplatform / Compose Multiplatform**) para coordenação de viagens em grupo, acompanhamento de rotas em tempo real, alertas geoespaciais de separação e sugestão de pontos de reencontro.

## Plataformas Suportadas

- **Android** (`androidMain`): Interface Material 3 com barra de navegação padrão Android e rastreamento GPS via `LocationManager`.
- **iOS** (`iosMain` + `iosApp`): Shell de navegação nativo via `UITabBarController` (entregando o efeito oficial **Apple Liquid Glass** ao compilar no Xcode 26 / iOS 26) e rastreamento via `CLLocationManager` (`CoreLocation`).
- **Web** (`wasmJsMain`): Versão compilada para WebAssembly com suporte a visualização no navegador.

## Principais Funcionalidades

- **Banco Geoespacial (Supabase + PostGIS)** (`supabase/schema.sql`):
  - Armazenamento de rotas com `geography(Point, 4326)` e índice espacial `GIST`.
  - Função RPC `check_trip_separation` utilizando `ST_Distance` e `ST_DWithin` para detectar afastamentos além do limite configurável pelo usuário.
  - Suporte ao modo de participantes em movimento (`MOVING_VEHICLE`, `WALKING_TRANSIT`) ou parados aguardando no destino (`STATIONARY_HOST`).
- **Autenticação**: Cadastro e login com e-mail/senha integrados ao `supabase-kt` Auth, com fallback local para testes offline.
- **Roteamento e Geocodificação** (`RouteSearchService`): Autocomplete com OpenStreetMap Nominatim e cálculo de distância/tempo viário via OSRM API.
- **Sala e Convites**: Geração dinâmica de código de convite e QR Code por sala, com cópia rápida para a área de transferência.

## Como Executar

### 1. Banco de Dados (Supabase)
1. Crie um projeto no Supabase e abra o **SQL Editor**.
2. Execute o script em [`supabase/schema.sql`](supabase/schema.sql) para habilitar o PostGIS, criar as tabelas e habilitar o Realtime.
3. Informe a *Project URL* e a *Anon Public Key* na aba **Configurações** dentro do app ou em [`SupabaseConfig.kt`](composeApp/src/commonMain/kotlin/com/letsdrive/app/data/remote/SupabaseConfig.kt).

### 2. Android
```bash
./gradlew :composeApp:assembleDebug
```

### 3. iOS (macOS / Xcode)
1. Clone o repositório no Mac:
   ```bash
   git clone https://github.com/Vini1227/lets-drive.git
   cd lets-drive
   ```
2. Abra o projeto no Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
3. Selecione seu iPhone ou Simulador, configure sua conta Apple em **Signing & Capabilities** e clique em **Run**.

### 4. Web (Wasm)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution
```
