# WeatherTEC 🌡️

Aplicación Android nativa (Kotlin + Jetpack Compose) que recolecta datos meteorológicos desde la API de Google Weather, los guarda en Firebase Realtime Database y los presenta en una interfaz moderna.

## Pantallas

| Pantalla | Descripción |
|---|---|
| **Inicio** | Condiciones actuales en vivo + resumen del día |
| **Por Hora** | Promedio por hora del día actual |
| **Rango** | Promedio de un rango de fechas |
| **Histórico** | Todos los registros de una fecha específica |
| **Comparar** | Comparación entre dos fechas |
| **Exportar** | Exportar datos como CSV |

## Setup

### 1. Clonar el repositorio
```bash
git clone https://github.com/Brxck2203/WeatherTEC_Project.git
cd WeatherTEC_Project
```

### 2. Configurar `local.properties`
Crea el archivo `local.properties` en la raíz (ya está en `.gitignore`):
```properties
GOOGLE_WEATHER_KEY=tu_api_key_de_google_weather
sdk.dir=/ruta/a/tu/android/sdk
```

### 3. Configurar Firebase
En `app/build.gradle.kts`, cambia `FIREBASE_BASE_URL` por la URL de tu Firebase Realtime Database si es diferente.

### 4. Abrir en Android Studio
Abre el proyecto en Android Studio (Iguana 2023.2.1+) y ejecuta en un emulador o dispositivo (API 24+).

## Stack Tecnológico

- **Kotlin** + **Jetpack Compose** — UI
- **Retrofit + OkHttp** — HTTP (Google Weather API + Firebase)
- **ViewModel + StateFlow** — manejo de estado
- **Navigation Compose** — navegación entre pantallas
- **Coroutines** — llamadas asíncronas

## Ubicación configurada

La app está configurada para el **Campus TEC** (San Carlos, Costa Rica):
- Latitud: `10.3643`
- Longitud: `-84.5097`

Puedes cambiarlos en `WeatherRepository.kt` (`LAT` y `LON`).
