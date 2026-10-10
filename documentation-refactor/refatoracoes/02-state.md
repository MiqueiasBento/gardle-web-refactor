# 02 — State para o status da locação

## 1. Problema existente

O `LeasingService` precisava conhecer todas as transições de status. As regras para proprietário e solicitante ficavam em dois blocos de condicionais. Ao criar uma nova transição, seria fácil esquecer a regra de permissão, o prazo ou o efeito no pagamento.

## 2. Trecho ou estrutura original

Antes desta mudança, `LeasingService.java` tinha `updateLeasingForRequester` e `updateLeasingForOwnerOfGardenField`. O primeiro permitia `OPEN → CANCELLED`, cancelando o pagamento. O segundo permitia `OPEN → RESERVED`, verificando o prazo e finalizando o pagamento, ou `OPEN → REJECTED`, cancelando o pagamento. Mudanças a partir de `RESERVED`, `REJECTED` e `CANCELLED` lançavam `InvalidLeasingStateServiceException`; repetir o status atual nesses estados era aceito. Para o proprietário, pedir `OPEN` quando já estava `OPEN` também gerava exceção.

```java
if (currentLeasing.getStatus().equals(LeasingStatus.OPEN)) {
    if (updatedLeasing.getStatus().equals(LeasingStatus.RESERVED)) {
        checkIsUpdateAllowedInPeriod(currentLeasing);
        currentLeasing.setStatus(LeasingStatus.RESERVED);
        paymentService.finalizePayment(currentLeasing);
    } else if (updatedLeasing.getStatus().equals(LeasingStatus.REJECTED)) {
        currentLeasing.setStatus(LeasingStatus.REJECTED);
        paymentService.cancelPayment(currentLeasing.getPaymentSessionId());
    }
}
```

## 3. Padrão selecionado

**State**. A interface `LeasingStatusBehavior` define a operação de transição. `OpenLeasingStatusBehavior` contém as três saídas de `OPEN` e `TerminalLeasingStatusBehavior` define o comportamento comum dos três estados terminais. `LeasingStatusBehaviors` escolhe o objeto pelo status atual. O nome evita confusão com `LeasingState`, que já representa o filtro temporal das consultas.

## 4. Justificativa da escolha

O comportamento de atualização depende do **status em que a locação está**, não apenas do status solicitado. `OPEN` permite ações diferentes para proprietário e solicitante; os demais estados impedem novas mudanças. Um objeto por comportamento deixa essa distinção visível e permite acrescentar outro status sem ampliar os condicionais do serviço.

## 5. Alteração realizada

O `LeasingService` continua verificando se o usuário é proprietário; para o solicitante, conserva a checagem de prazo e de permissão antes da transição. Depois delega ao comportamento do status atual. A implementação de `OPEN` aplica a regra do papel, atualiza o status e chama `PaymentService` para cancelar ou finalizar quando necessário. O serviço continua responsável por salvar a locação e criar a notificação. As assinaturas públicas não mudaram.

## 6. Estrutura resultante

```text
LeasingService.updateLeasing
  -> verifica papel e, para solicitante, prazo/permissão
  -> LeasingStatusBehaviors.forStatus(status atual)
     -> OpenLeasingStatusBehavior: CANCELLED / RESERVED / REJECTED
     -> TerminalLeasingStatusBehavior: mesmo status ou exceção
  -> salva e notifica
```

## 7. Benefícios obtidos

As regras de `OPEN` ficaram juntas e os três estados terminais compartilham uma implementação. O `LeasingService` não precisa mais decidir cada transição. Os testes exercitam cada saída, cada proibição e os efeitos reais de `PaymentService` sobre um `PaymentProviderService` mockado. Isso torna as futuras mudanças de ciclo de vida mais localizadas e verificáveis.

## Validação

- **Antes:** `LeasingServiceTest`: 15 testes, 0 falhas, 0 erros, `BUILD SUCCESS`.
- **Depois:** `LeasingServiceTest` + `LeasingStatusBehaviorTest`: 23 testes, 0 falhas, 0 erros, `BUILD SUCCESS`. O novo teste cobre as três transições válidas, prazo da reserva, exceções por papel, todos os estados terminais e repetição do status atual.
- Comando: `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ./mvnw -o -Dmaven-surefire-plugin.version=3.5.6 -Dskip.npm -Dskip.installnodenpm -Dtest=LeasingStatusBehaviorTest,LeasingServiceTest test`.
- **Suíte completa após a mudança:** 79 testes, 0 falhas, 0 erros, `BUILD SUCCESS`, com o mesmo comando acima sem `-Dtest=...`.
