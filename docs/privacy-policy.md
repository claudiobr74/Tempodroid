# Política de Privacidade — Tempodroid

**Última atualização: 17 de setembro de 2026**

O Tempodroid é um aplicativo Android focado em um widget de relógio, clima e ciclo Sol/Lua. Esta política explica quais dados são usados e para quê.

## Dados usados pelo aplicativo

### Localização aproximada

A localização aproximada é **opcional**. O Tempodroid só solicita a permissão `ACCESS_COARSE_LOCATION` quando o usuário toca em **Usar localização aproximada**.

Quando autorizada, a última localização aproximada disponível no aparelho pode ser usada para definir a cidade e consultar o clima correspondente. O Tempodroid não solicita localização precisa, não usa localização em segundo plano e não mantém rastreamento contínuo.

### Cidade e coordenadas escolhidas

O usuário também pode escolher uma cidade manualmente. Para cada instância do widget, o aplicativo armazena localmente:

- cidade selecionada;
- latitude e longitude;
- fuso horário;
- origem da localização escolhida;
- último clima válido e dados astronômicos necessários ao widget.

Esses dados ficam no armazenamento local do aplicativo e não são enviados para servidores próprios do Tempodroid.

## Serviços externos

Para fornecer previsão e condições meteorológicas, o Tempodroid envia as coordenadas da cidade selecionada para a **Open-Meteo**. A busca manual de cidades também utiliza o serviço de geocodificação da Open-Meteo, cuja base de localização utiliza dados do **GeoNames**.

Esses serviços podem receber informações técnicas inerentes a uma conexão de internet, como endereço IP, conforme suas próprias políticas e termos.

Referências:

- Open-Meteo: https://open-meteo.com/
- Termos e privacidade da Open-Meteo: https://open-meteo.com/en/terms
- GeoNames: https://www.geonames.org/

## Finalidades

Os dados são usados somente para:

- selecionar a localização meteorológica;
- obter o clima atual e o resumo diário;
- calcular e exibir nascer e pôr do Sol;
- calcular e exibir informações da Lua;
- manter o widget funcional mesmo temporariamente sem internet.

## Publicidade, contas e analytics

Na versão 1.0.0, o Tempodroid:

- não possui anúncios;
- não exige conta ou login;
- não vende dados pessoais;
- não possui SDK de publicidade;
- não possui SDK de analytics;
- não cria perfil comportamental do usuário.

## Retenção e exclusão

Os dados de configuração e cache permanecem apenas no aparelho enquanto forem necessários ao funcionamento do widget.

Ao remover uma instância do widget, os dados daquela instância são apagados do armazenamento do aplicativo. O usuário também pode apagar todos os dados pelo sistema Android ou desinstalar o aplicativo.

O Tempodroid desativa o backup automático dos dados do aplicativo.

## Segurança

As comunicações externas utilizam HTTPS. O aplicativo bloqueia tráfego HTTP em texto claro.

## Crianças

O Tempodroid não foi projetado para coletar intencionalmente dados pessoais de crianças e não possui recursos sociais, publicidade personalizada ou criação de conta.

## Alterações desta política

Esta política poderá ser atualizada quando houver mudança relevante no funcionamento do aplicativo, em fornecedores externos ou em requisitos legais e da Google Play.

## Contato

Dúvidas, solicitações ou relatos relacionados ao Tempodroid podem ser enviados pelo repositório oficial:

https://github.com/claudiobr74/Tempodroid/issues
