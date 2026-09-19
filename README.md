# Tempodroid

Widget Android 4×2 de clima com céu dinâmico, ciclo solar/lunar, timeline e suporte a múltiplas instâncias configuráveis.

## Fonte visual canônica

O Figma `Tempodroid` é a fonte de verdade visual. Na implementação atual:

- `DynamicSkyBackground` fornece os 10 backgrounds reutilizáveis de runtime (ClearDay, PartlyCloudy, Cloudy, Rain, Storm, Sunrise, Sunset, ClearNight, CloudyNight e RainNight). Os bytes desses assets são verificados no CI.
- `09 · Refined Widget Previews v2` define a composição final da timeline: apenas Nascer, Agora e Ocaso; badge circular do Agora; ícones meteorológicos coloridos; proporções, hierarquia e regras visuais de colisão.
- A Lua continua astronômica/dinâmica para preservar fase, hemisfério, posição e oclusão por nuvens. Os previews da seção 09 são referência visual; fotos de apresentação que contenham uma Lua já incorporada não são usadas como background de runtime, evitando duplicar a Lua e perder a dinâmica astronômica.

## Desenvolvimento

O projeto usa Kotlin/Android, Glance AppWidget e módulos `app` + `core`.

### Validação

```bash
./gradlew :core:test :app:test :app:lintRelease :app:assembleDebug :app:bundleRelease
python3 scripts/verify_figma_visual_assets.py
```

O CI também publica previews da timeline/Lua, um APK debug instalável, um AAB de validação não assinado para produção e o mapping do R8.

## Privacidade

Consulte a tela de Política de Privacidade no aplicativo e os arquivos de documentação do repositório.
