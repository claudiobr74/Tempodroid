# Tempodroid — Play Store Release Guide

## Package e versão inicial

- `applicationId`: `com.tempodroid.myapp`
- versão: `1.0.0`
- `versionCode`: `1`
- `minSdk`: 26
- `targetSdk`: 36
- formato oficial do widget: 4×2

O `applicationId` deve ser tratado como definitivo após o primeiro envio ao Google Play.

> Observação: o `namespace` Kotlin permanece `com.tempodroid` por decisão arquitetural. O identificador público/Play Store é `com.tempodroid.myapp`; não é necessário renomear os pacotes-fonte para que o bundle pertença a esse app.

## Build local de validação

```bash
./gradlew :core:test :app:test :app:lintRelease :app:assembleDebug :app:bundleRelease
```

O bundle de release não assinado é criado em:

`app/build/outputs/bundle/release/app-release.aab`

## Play App Signing e upload key

Use o **Play App Signing** e mantenha apenas a upload key sob controle do desenvolvedor.

Para criar uma upload key nova em uma máquina confiável:

```bash
keytool -genkeypair -v \
  -keystore tempodroid-upload.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Guarde o arquivo e as senhas em local seguro, fora do repositório.

Nunca faça commit de:

- `.jks` / `.keystore`;
- senhas;
- `keystore.properties`;
- AAB/APK de produção.

O projeto aceita versão e assinatura via variáveis de ambiente:

- `TEMPODROID_VERSION_CODE`
- `TEMPODROID_VERSION_NAME`
- `TEMPODROID_KEYSTORE_PATH`
- `TEMPODROID_KEYSTORE_PASSWORD`
- `TEMPODROID_KEY_ALIAS`
- `TEMPODROID_KEY_PASSWORD`

Exemplo:

```bash
export TEMPODROID_VERSION_CODE=1
export TEMPODROID_VERSION_NAME=1.0.0
export TEMPODROID_KEYSTORE_PATH=/caminho/tempodroid-upload.jks
export TEMPODROID_KEYSTORE_PASSWORD='***'
export TEMPODROID_KEY_ALIAS='upload'
export TEMPODROID_KEY_PASSWORD='***'
./gradlew :app:bundleRelease
```

## GitHub Actions — AAB assinado

Workflow manual:

`.github/workflows/release-aab.yml`

Configure os seguintes GitHub Actions Secrets:

- `TEMPODROID_UPLOAD_KEYSTORE_BASE64`
- `TEMPODROID_KEYSTORE_PASSWORD`
- `TEMPODROID_KEY_ALIAS`
- `TEMPODROID_KEY_PASSWORD`

Para gerar o primeiro secret:

```bash
base64 -w 0 tempodroid-upload.jks
```

No macOS:

```bash
base64 tempodroid-upload.jks | tr -d '\n'
```

Depois execute **Build signed Play Store AAB** em Actions → Run workflow, preenchendo `version_code` e `version_name`.

Nunca reutilize um `versionCode` já enviado ao Play Console. Cada nova versão precisa de um número maior.

## Checklist antes do upload

- CI verde.
- `lintRelease` verde.
- `bundleRelease` verde.
- AAB assinado com upload key correta.
- Teste de instalação via Internal App Sharing ou faixa de teste interno.
- Widget adicionado/cancelado/configurado corretamente.
- Dois widgets com cidades diferentes permanecem isolados.
- Remover uma instância não afeta outra.
- Permissão de localização é opcional e só aparece após ação explícita.
- Modo offline mantém o último clima válido.
- Testes em Android 8 (API 26), Android 12, Android 14 e Android 16.
- Pelo menos um launcher Google/Pixel, Samsung One UI e Xiaomi/HyperOS quando disponíveis.
- Política de privacidade publicada em URL HTTPS pública e sem login.
- Política contém nome do app/desenvolvedor e um contato público de privacidade.
- Formulário Segurança dos dados revisado.
- Classificação indicativa preenchida.
- Declaração de anúncios: sem anúncios na versão 1.0.0.
- Categoria sugerida: Clima.
- E-mail de suporte válido configurado no Play Console.
- Screenshots reais do aplicativo/widget, não mockups, enviados à ficha da loja.

## Open-Meteo

A versão gratuita da API deve ser usada apenas dentro das condições vigentes do fornecedor. O Tempodroid inclui atribuição no aplicativo e no arquivo `NOTICE.md`.

Se o aplicativo passar a ter anúncios, assinatura, venda ou outra finalidade comercial, revisar o plano/licença da Open-Meteo antes de publicar a alteração.

## Metadados da loja

Textos preparados em:

`fastlane/metadata/android/pt-BR/`

A política de privacidade está em:

`docs/privacy-policy.md`

A versão HTML pronta para hospedagem está em:

`docs/privacy-policy.html`

O rascunho do formulário Segurança dos dados está em:

`docs/data-safety.md`

As declarações sugeridas do Play Console estão em:

`docs/play-console-declarations.md`

O plano de screenshots e feature graphic está em:

`store-assets/README.md`
