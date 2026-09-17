# Tempodroid

App Android com o widget **SolarWeatherWidget** 4×2: relógio + clima + linha do tempo Sol/Lua.

Conceito visual: [Figma Tempodroid](https://www.figma.com/design/jRSUUY52kmf1xtuoZLQQaI/Tempodroid).

## Stack

- Kotlin, Jetpack Glance, AppWidget, WorkManager, DataStore
- Canvas/Bitmap para céu, Sol, Lua e timeline
- Módulo `:core` (JVM) com domínio e cálculos testáveis
- Open-Meteo como `WeatherProvider` (UI não conhece o provider)

## Relógio

O widget **não** redesenha a cada minuto. A hora usa `TextClock` via `AndroidRemoteViews` integrado ao Glance, atualizada pelo launcher. Clima, céu e timeline seguem o WorkManager periódico de 30 minutos (e criação/boot/app/localização). Sem serviço persistente e sem polling por minuto.

## Astronomia

Cálculo **local**, sem biblioteca externa:

- Sol: algoritmo NOAA Solar Calculator
- Lua: termos principais de Meeus (fase, iluminação, altitude) + moonrise/moonset por amostragem
- Hemisfério sul (Brasil) inverte a orientação da face iluminada

## Build

```bash
./gradlew :core:test :app:test :app:assembleDebug
```

Permissões: `INTERNET` obrigatória; `ACCESS_COARSE_LOCATION` opcional. Fallback: Goiânia.
