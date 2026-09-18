# Auditoria visual Figma → Android

Data da auditoria: 2026-09-18
Arquivo Figma: `jRSUUY52kmf1xtuoZLQQaI`

## Resultado

A matriz `MoonWeatherSky` foi varrida integralmente: 120 variantes (5 condições × 3 períodos × 8 fases lunares). O mapeamento lógico do app para `DAY`, `TWILIGHT` e `NIGHT` coincide com o Figma. O defeito encontrado estava nos nomes dos rasters importados: 7 dos 10 backgrounds estavam associados ao arquivo errado, apesar de todos os PNGs originais do Figma estarem presentes no repositório.

A correção reutiliza os bytes originais já versionados no Git, apenas reposicionando cada blob no nome semântico correto. Nenhuma imagem foi recriada, recomprimida ou gerada por IA.

## Fonte de verdade: DynamicSkyBackground

| Estado Figma | Node | imageHash Figma | Bytes | Arquivo Android correto | Git blob correto |
|---|---:|---|---:|---|---|
| ClearDay | `4:542` | `e68fe5142deb194f016900b832284dc8f3fbcfd8` | 993364 | `figma_sky_clear_day.png` | `423dc248aa98608c7d188376c9da1e130d06a5fb` |
| PartlyCloudy | `4:543` | `49ec8564bb1de423c9726222463d96a965700e56` | 987746 | `figma_sky_partly_cloudy.png` | `83e38c7bf8322ae0d1cc44e1a6137e16463cc6f8` |
| Cloudy | `4:544` | `8509ea9a4610046b4032aa64be1193c83c623328` | 1051854 | `figma_sky_cloudy.png` | `f4e7727444478efd7df04f2877f6f52f42f10597` |
| Rain | `4:545` | `71f8b58a1376a496690d9ca5fcae318268ca7326` | 871409 | `figma_sky_rain.png` | `d12535fff4a935a657d1c01f57d5cd8412efaa38` |
| Storm | `4:546` | `1edaa05e816015daddd8889b1306d40db256a569` | 909152 | `figma_sky_storm.png` | `2b077650818b4312d7d264302ac8312b53905871` |
| Sunrise | `4:548` | `a794bdba4031c9b195ecf4f145a0ad0f14456481` | 1107678 | `figma_sky_sunrise.png` | `e8d3aa2c9739bf53c023069822e3e88843b4fd49` |
| Sunset | `4:549` | `816e648ce4c5797d0443b6fb6c04c6e54802507a` | 906815 | `figma_sky_sunset.png` | `ad0c8b764d78ccc9894946389f8afcaa3d46de1e` |
| ClearNight | `4:550` | `33e664c4b97a1749240e57e9af870a9b8eec44ac` | 870766 | `figma_sky_clear_night.png` | `62155b1d1ec41139eaf7aaa34b5797af7087a75f` |
| CloudyNight | `4:551` | `c64c0c304b9389724da0dd5d6c16f248492095dd` | 768856 | `figma_sky_cloudy_night.png` | `b96f59303754138b59503f42b476ae153074323f` |
| RainNight | `4:553` | `c7ddd72bccaafc8b96b5c99feeed9c0962ee34aa` | 1183653 | `figma_sky_rain_night.png` | `ae53a54c75a226da70d94bca2ab37daf3f88c366` |

Textura lunar: Figma imageHash `c7a040c427aaddbd861c78b25918648209860fa1`, 151644 bytes, arquivo `figma_moon_surface.png`, Git blob `2e3fa1830c3be5161e773011e3fb0e7ed9a1b395`.

## Swaps detectados na versão anterior

- `figma_sky_clear_day.png` continha **PartlyCloudy**.
- `figma_sky_partly_cloudy.png` continha **RainNight**.
- `figma_sky_cloudy.png` estava correto.
- `figma_sky_rain.png` estava correto.
- `figma_sky_storm.png` continha **ClearNight**.
- `figma_sky_sunrise.png` estava correto.
- `figma_sky_sunset.png` continha **ClearDay**.
- `figma_sky_clear_night.png` continha **CloudyNight**.
- `figma_sky_cloudy_night.png` continha **Storm**.
- `figma_sky_rain_night.png` continha **Sunset**.

## Matriz verificada no Figma

A inspeção programática de todas as 120 variantes confirmou:

- DAY: clear→ClearDay, partly-cloudy→PartlyCloudy, cloudy→Cloudy, rain→Rain, storm→Storm.
- TWILIGHT: clear/partly-cloudy→Sunset, cloudy→CloudyNight, rain→RainNight, storm→Storm.
- NIGHT: clear→ClearNight, partly-cloudy/cloudy→CloudyNight, rain→RainNight, storm→Storm.

Opacidades da Lua também foram verificadas nas 120 variantes e permanecem alinhadas ao Figma. O estado `DAWN` do app continua usando `Sunrise` para clear/partly-cloudy e os estados meteorológicos próprios para cloudy/rain/storm.

## Proteção contra regressão

`scripts/verify_figma_visual_assets.py` calcula o Git blob id de cada raster e falha se qualquer arquivo for alterado ou associado ao nome errado. Esse gate roda no CI e na geração do AAB assinado.
