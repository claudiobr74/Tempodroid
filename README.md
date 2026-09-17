# Tempodroid

App Android com widget **Tempodroid — Clima 4×2**: relógio + clima + linha do tempo Sol/Lua.

Conceito visual: [Figma Tempodroid](https://www.figma.com/design/jRSUUY52kmf1xtuoZLQQaI/Tempodroid).

## Stack

- Kotlin, Jetpack Glance, AppWidget, WorkManager, DataStore
- Canvas/Bitmap para céu, Sol, Lua e timeline
- Módulo `:core` (JVM) com domínio e cálculos testáveis
- Open-Meteo atrás da interface `WeatherProvider`
- Android 8+ (`minSdk 26`), compilação/target Android 16 (`API 36`)

## Relógio e atualização

O widget não redesenha a cada minuto. A hora usa `TextClock` via `AndroidRemoteViews` integrado ao Glance. Clima, céu e timeline seguem WorkManager periódico de 30 minutos, além de refresh após configuração e eventos relevantes. Não há serviço persistente nem polling por minuto.

## Astronomia

Cálculo local:

- Sol: algoritmo NOAA Solar Calculator
- Lua: termos principais de Meeus, fase, iluminação, altitude e moonrise/moonset
- orientação visual compatível com hemisfério sul e norte

## Privacidade

- localização aproximada opcional;
- sem `ACCESS_FINE_LOCATION`;
- sem localização em segundo plano;
- sem anúncios, login ou analytics na versão 1.0.0;
- backup automático dos dados desativado;
- HTTPS e tráfego em texto claro bloqueado.

Veja `docs/privacy-policy.md` e `docs/data-safety.md`.

## Build de validação para release

```bash
./gradlew :core:test :app:test :app:lintRelease :app:assembleDebug :app:bundleRelease
```

O CI executa esse gate em cada pull request. O AAB assinado para Play Store é gerado pelo workflow manual `Build signed Play Store AAB`, usando a upload key armazenada somente em GitHub Actions Secrets.

Instruções completas: `docs/play-store-release.md`.

## Dados meteorológicos

Dados meteorológicos: **Open-Meteo** (atribuição CC BY 4.0). Busca de cidades: Open-Meteo, baseada em dados GeoNames. Veja `NOTICE.md`.
