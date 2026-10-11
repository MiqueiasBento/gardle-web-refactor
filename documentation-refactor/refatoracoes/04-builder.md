# 04 — Builder para a requisição de checkout da Stripe

## 1. Problema existente

Para abrir um checkout, `StripeService` montava cinco blocos de dados com vários `Map` e listas. O método misturava informações da locação com nomes de campos exigidos pela Stripe. Uma mudança no payload obrigava quem lê o serviço a acompanhar a montagem inteira para entender o que seria enviado.

## 2. Trecho ou estrutura original

O método `createCheckoutSessionRequestParamsFromCreatorLeasingDTO`, em `StripeService.java`, criava `paymentIntentData`, `paymentIntentMetaData`, `paymentMethodTypes`, `lineItems` e `params`. Por exemplo:

```java
paymentIntentData.put("capture_method", "manual");
paymentIntentMetaData.put("leasing_from", creatorLeasingDTO.getFrom().toString());
lineItem1.put("amount", creatorLeasingMapper.toEntity(creatorLeasingDTO).getPriceSumInCents());
params.put("payment_intent_data", paymentIntentData);
```

Essas chaves são importantes para a chamada à Stripe, mas ocupavam a lógica de orquestração do serviço.

## 3. Padrão selecionado

**Builder**. `CheckoutSessionRequestBuilder` recebe URLs, metadados da locação e dados do item por métodos próprios. `build()` verifica os campos obrigatórios e produz o `Map<String, Object>` que o `StripeClient` já espera.

## 4. Justificativa da escolha

O checkout é uma requisição composta por partes relacionadas e mapas aninhados. Métodos com nomes de domínio tornam as etapas de construção legíveis, enquanto as chaves da API ficam em um só lugar. O builder ficou específico para checkout: a requisição de criação de conta tem outra estrutura e não foi alterada nesta etapa.

## 5. Alteração realizada

`StripeService` ainda obtém o usuário, calcula o preço com `CreatorLeasingMapper` e chama `StripeClient.createCheckoutSession`. A montagem do payload foi transferida para o builder. Permaneceram iguais os nomes das chaves, `card`, `eur`, a captura `manual`, a quantidade `1`, os quatro metadados e as duas URLs configuradas. O método público e o cliente Stripe não mudaram.

## 6. Estrutura resultante

```text
StripeService.createCheckoutSession
  -> CheckoutSessionRequestBuilder
       .urls(...)
       .leasingMetadata(...)
       .lineItem(...)
       .build()
  -> StripeClient.createCheckoutSession(Map)
```

## 7. Benefícios obtidos

A montagem da requisição agora tem um lugar próprio. O serviço mostra quais informações fornece sem carregar detalhes dos mapas aninhados. Uma requisição incompleta falha em `build()` antes da chamada externa. A separação também permite testar o payload enviado à Stripe sem fazer uma chamada de rede.

## Validação

- **Antes da refatoração:** os 7 testes existentes de `StripeServiceTest` passaram. Foi acrescentado um teste de caracterização que capturou o `Map` enviado ao `StripeClient`; os 8 testes passaram no código original.
- **Depois da refatoração:** os mesmos 8 testes passaram, inclusive a verificação de URLs, moeda, valor em centavos, captura manual e metadados. Um teste puro adicional verifica a recusa de payload sem identificador do solicitante.
- Comando com JDK 8: `./mvnw -o -Dmaven-surefire-plugin.version=3.5.6 -Dskip.npm -Dskip.installnodenpm test` (com `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`).
- **Suíte completa após a implementação:** 82 testes, 0 falhas, 0 erros, `BUILD SUCCESS`.
