# 03 — Observer para avisos de locação

## 1. Problema existente

O `LeasingService` chamava o serviço de mensagens diretamente após salvar uma locação. Além de cuidar das regras e da persistência, ele precisava saber quem enviaria o aviso. Se surgisse outra reação à criação ou à atualização, como auditoria, a classe ganharia mais uma dependência e mais chamadas nos dois fluxos.

## 2. Trecho ou estrutura original

Logo após `leasingRepository.save(...)`, tanto `createLeasing` quanto `updateLeasing` faziam:

```java
messageService.createLeasingNotification(leasing);
```

A notificação acontecia dentro da chamada principal, antes de devolver o DTO. Se ela falhasse, a exceção chegava ao chamador.

## 3. Padrão selecionado

**Observer**, usando o mecanismo de eventos do Spring. `LeasingChangedEvent` carrega a locação salva; `ApplicationEventPublisher` anuncia o evento; `LeasingNotificationListener` o observa por meio de `@EventListener` e chama `MessageService`.

## 4. Justificativa da escolha

O envio do aviso é uma reação à mudança da locação, e já ocorre em dois pontos. Publicar um evento permite acrescentar outro observador sem alterar esses dois fluxos. O listener continua síncrono e participa da mesma transação do serviço: essa escolha preserva o momento da notificação e a propagação de falhas do código original.

## 5. Alteração realizada

As duas chamadas diretas foram trocadas por `eventPublisher.publishEvent(new LeasingChangedEvent(leasingSalva))`. A dependência de `MessageService` saiu do `LeasingService` e ficou no novo listener. Não foi usado `@Async` nem `@TransactionalEventListener`. A persistência, a criação do DTO e as regras de status não mudaram.

## 6. Estrutura resultante

```text
LeasingService.createLeasing / updateLeasing
  -> salva a locação
  -> publica LeasingChangedEvent
     -> LeasingNotificationListener.onLeasingChanged (@EventListener)
        -> MessageService.createLeasingNotification
  -> devolve o DTO
```

## 7. Benefícios obtidos

O serviço de locações não conhece mais o serviço de mensagens. O motivo do aviso ficou explícito no evento, e futuras reações podem ser adicionadas por novos listeners. A execução continua imediata; o teste verifica que a notificação ocorre na transação e que uma falha dela interrompe a chamada original.

## Validação

- **Antes:** `LeasingServiceTest`: 15 testes, 0 falhas, 0 erros, `BUILD SUCCESS`.
- **Depois:** `LeasingServiceTest`: 15 testes, 0 falhas, 0 erros, `BUILD SUCCESS` após a troca inicial. Os testes de criação e atualização verificam que a notificação foi recebida; o de criação também verifica transação ativa no instante da chamada.
- Foi incluído um caso que faz `MessageService` lançar exceção para verificar a propagação síncrona. O resultado da suíte completa está registrado abaixo.
- **Suíte completa após o novo caso:** 80 testes, 0 falhas, 0 erros, `BUILD SUCCESS`. O comando usou JDK 8: `./mvnw -o -Dmaven-surefire-plugin.version=3.5.6 -Dskip.npm -Dskip.installnodenpm test`.
