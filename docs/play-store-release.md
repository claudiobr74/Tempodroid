# Tempodroid — Play Store Release Guide

## Package e configuração

- `applicationId`: `com.tempodroid.myapp`
- versão pública atual: `1.0.0`
- `minSdk`: 26
- `targetSdk`: 36
- formato oficial do widget: 4×2

O `applicationId` é definitivo para a publicação existente no Google Play.

> Observação: o `namespace` Kotlin permanece `com.tempodroid` por decisão arquitetural. O identificador público/Play Store é `com.tempodroid.myapp`; não é necessário renomear os pacotes-fonte para que o bundle pertença a esse app.

## Controle de versionCode

O último `versionCode` de produção emitido é registrado em:

`release/play-version-code.txt`

O workflow de release recusa automaticamente qualquer código menor ou igual a esse valor. Depois que um novo AAB for efetivamente emitido/aceito como próximo release, atualizar esse arquivo para o código correspondente antes da release seguinte.

Nunca reutilize um `versionCode` já enviado ao Play Console.

## Build local de validação

```bash
./gradlew :core:test :app:test :app:lintRelease :app:assembleDebug :app:bundleRelease
```

O bundle de release é criado em:

`app/build/outputs/bundle/release/app-release.aab`

## Play App Signing e upload key

Use o **Play App Signing** e mantenha apenas a upload key sob controle do desenvolvedor.

Guarde o arquivo de upload e as senhas em local seguro, fora do repositório. Nunca faça commit de:

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

## GitHub Actions — AAB assinado

Workflow manual:

`.github/workflows/release-aab.yml`

Secrets necessários:

- `TEMPODROID_UPLOAD_KEYSTORE_BASE64`
- `TEMPODROID_KEYSTORE_PASSWORD`
- `TEMPODROID_KEY_ALIAS`
- `TEMPODROID_KEY_PASSWORD`

Execute **Build signed Play Store AAB** em Actions → Run workflow, preenchendo `version_code` e `version_name`. O workflow valida o código, testa `core` e `app`, executa `lintRelease`, gera o bundle assinado e preserva o R8 mapping.

## Checklist antes do upload

- CI verde.
- `lintRelease` verde.
- `bundleRelease` verde.
- `versionCode` maior que o valor registrado em `release/play-version-code.txt`.
- AAB assinado com a upload key correta.
- Teste de instalação via Internal App Sharing ou faixa de teste interno.
- Widget adicionado/cancelado/configurado corretamente.
- Dois widgets com cidades diferentes permanecem isolados.
- Remover a última instância cancela o trabalho periódico e o alarme astronômico.
- Permissão de localização é opcional e só aparece após ação explícita.
- Modo offline mantém o último clima válido.
- Redesenho astronômico não dispara consulta meteorológica por si só.
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

A API deve ser usada dentro das condições vigentes do fornecedor. O Tempodroid inclui atribuição no aplicativo e no arquivo `NOTICE.md`.

Se o aplicativo passar a ter anúncios, assinatura, venda ou outra finalidade comercial, revisar o plano/licença da Open-Meteo antes de publicar a alteração.

## Metadados da loja

Textos preparados em:

`fastlane/metadata/android/pt-BR/`

Política de privacidade:

- `docs/privacy-policy.md`
- `docs/privacy-policy.html`

Segurança dos dados:

- `docs/data-safety.md`
- `docs/play-console-declarations.md`

Assets da loja:

- `store-assets/README.md`
