# Rascunho — Segurança dos dados (Google Play)

Este documento serve como checklist para preencher o formulário **Segurança dos dados** no Play Console. A declaração final deve refletir exatamente a versão publicada.

## Visão geral da versão 1.0.0

- Sem conta/login.
- Sem anúncios.
- Sem analytics.
- Sem SDK de publicidade.
- Sem compra no app.
- Sem localização precisa.
- Sem localização em segundo plano.
- Localização aproximada opcional e solicitada apenas após ação explícita do usuário.
- Cidade, coordenadas, timezone, clima em cache e astronomia ficam armazenados localmente no aparelho.
- Coordenadas da cidade selecionada são enviadas à Open-Meteo para obter o clima.
- Termos de busca de cidade são enviados à API de geocodificação da Open-Meteo.

## Dados pessoais

### Localização aproximada

**Coletado:** sim, quando o usuário opta por usar localização aproximada ou escolhe uma cidade que é convertida em coordenadas.

**Obrigatório:** não.

**Finalidade:** funcionalidade do aplicativo — fornecer clima e dados Sol/Lua para a localização escolhida.

**Processamento:** armazenado localmente no aparelho e enviado ao serviço meteorológico para retornar os dados correspondentes.

**Compartilhamento com terceiros:** declarar de forma conservadora no Play Console como compartilhamento com o provedor meteorológico Open-Meteo, salvo se a definição vigente do formulário e a relação contratual aplicável permitirem classificar o fornecedor apenas como prestador de serviço excluído de “compartilhamento”.

**Venda:** não.

### Informações pessoais, contatos, fotos, áudio, arquivos, saúde, finanças e identificadores publicitários

O Tempodroid não solicita nem processa intencionalmente essas categorias na versão 1.0.0.

## Segurança

- Tráfego de rede por HTTPS.
- `usesCleartextTraffic=false`.
- Backup automático do aplicativo desativado.
- Sem chaves privadas ou credenciais de produção no repositório.

## Exclusão

Não há conta remota. Dados locais podem ser removidos por:

1. excluir a instância do widget, que remove o estado daquela instância;
2. limpar os dados do aplicativo no Android;
3. desinstalar o aplicativo.

## Revisar antes de enviar

Se qualquer uma destas funcionalidades for adicionada, revisar o formulário antes da publicação:

- analytics;
- crash reporting de terceiros;
- anúncios;
- assinatura ou compra;
- login;
- notificações com identificadores remotos;
- backend próprio;
- localização precisa ou em segundo plano;
- novo provedor meteorológico.
