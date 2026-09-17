# Tempodroid — Declarações sugeridas no Play Console

Revisar este arquivo imediatamente antes do envio da versão 1.0.0. As respostas abaixo refletem o código e o modelo de negócio atuais.

## Acesso ao app

**Todos os recursos estão disponíveis sem acesso especial.**

- Não há login.
- Não há assinatura.
- Não há conteúdo restrito por conta.
- A configuração principal acontece ao adicionar o widget Android 4×2.

Instrução útil para revisão:

> Abra o app Tempodroid e toque em “Adicionar widget à tela inicial”, ou adicione o widget pelo seletor de widgets do launcher. A cidade pode ser escolhida manualmente sem conceder localização. A permissão de localização aproximada é opcional.

## Anúncios

**O app contém anúncios? Não.**

A versão 1.0.0 não inclui SDK de publicidade nem publicidade própria.

## Público-alvo

O Tempodroid não é um produto desenhado especificamente para crianças. Não usar ilustrações, personagens, textos ou materiais de marketing com apelo infantil.

Selecionar no Play Console somente as faixas etárias que correspondam ao público realmente pretendido. Se qualquer faixa infantil for incluída, revisar integralmente a Política para Famílias antes do envio.

## Classificação de conteúdo

Responder ao questionário IARC de acordo com o produto real:

- sem violência;
- sem conteúdo sexual;
- sem linguagem imprópria;
- sem drogas/álcool;
- sem jogos de azar;
- sem interação entre usuários;
- sem compartilhamento social;
- sem compras digitais na versão 1.0.0.

A classificação final é atribuída pelo questionário do Play Console e não deve ser inventada no código ou na ficha da loja.

## Permissões

Permissões do aplicativo:

- `INTERNET`: obter clima e buscar cidades;
- `ACCESS_NETWORK_STATE`: determinar disponibilidade de rede e manter fallback offline;
- `ACCESS_COARSE_LOCATION`: localização aproximada opcional, somente após ação do usuário;
- `RECEIVE_BOOT_COMPLETED`: reagendar/atualizar o widget após reinicialização e eventos de sistema compatíveis.

Não há:

- `ACCESS_FINE_LOCATION`;
- localização em segundo plano;
- câmera;
- microfone;
- contatos;
- telefone/SMS;
- armazenamento amplo;
- identificador de publicidade.

## Segurança dos dados

Usar `docs/data-safety.md` como fonte de verdade para o preenchimento.

Ponto principal: localização aproximada é opcional, mas quando usada ou quando uma cidade é escolhida, coordenadas são transmitidas à Open-Meteo para entregar a funcionalidade meteorológica. Declarar isso de forma transparente.

## Política de privacidade

Antes de enviar para revisão, publicar `docs/privacy-policy.html` em uma URL HTTPS pública, estável e acessível sem login.

A política também está disponível dentro do próprio aplicativo em **Política de privacidade**.

## Exclusão de dados

Não existe conta remota. O estado de cada widget é apagado ao remover aquela instância. Todos os dados locais podem ser apagados em:

Android → Configurações → Apps → Tempodroid → Armazenamento → Limpar dados

ou ao desinstalar o aplicativo.

## Modelo de negócio / Open-Meteo

A versão 1.0.0 descrita nestes documentos é sem anúncios, sem assinatura e sem monetização. Antes de qualquer monetização, revisar contrato/plano de uso da Open-Meteo e atualizar política, Segurança dos dados e ficha da loja, se necessário.

## Categoria sugerida

**Clima**.

## Pré-requisitos externos antes de Produção

- conta de desenvolvedor Google Play verificada;
- identidade/organização verificadas conforme aplicável;
- package `com.tempodroid` registrado e aceito pela Play Console;
- Play App Signing habilitado;
- upload key criada e guardada com segurança;
- política de privacidade em URL pública;
- e-mail público de suporte configurado na ficha;
- screenshots reais do app/widget;
- feature graphic e ícone de alta resolução;
- teste interno/fechado concluído quando exigido para a conta.
