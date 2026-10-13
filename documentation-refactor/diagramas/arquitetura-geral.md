# Arquitetura geral e fluxo de uma locação

Representação do código atual na branch `refactor`. Situa o Strategy no fluxo de **consulta** e mostra, em paralelo, como uma locação é criada após o checkout. As setas indicam chamadas ou eventos observados no código; o diagrama não representa todos os endpoints do sistema.

```mermaid
flowchart LR
    U[Cliente web]
    STRIPE[Stripe]
    DB[(Banco de dados)]
    MAIL[Servidor de e-mail]

    subgraph API[Camada REST]
        GFC[GardenFieldController]
        UC[UserController]
        UGC[UserGardenFieldController]
        PC[PaymentController]
        SC[StripeController]
        LC[LeasingController]
    end

    subgraph SERVICE[Serviços e regras]
        LS[LeasingService]
        FILTER[LeasingState / QueryInstants]
        PS[PaymentService]
        SS[StripeService / StripeClient]
        WH[StripeWebhookService]
        EVENT[LeasingChangedEvent]
        LISTENER[LeasingNotificationListener]
        MSG[MessageService]
        US[UserService]
        MS[MailService]
    end

    subgraph DATA[Persistência]
        LR[LeasingRepository]
        MR[MessageRepository]
        UR[UserRepository]
    end

    U -->|consultas temporais| GFC
    U -->|locações por usuário| UC
    U -->|locações por proprietário| UGC
    GFC --> LS
    UC --> LS
    UGC --> LS
    LS -->|seleciona filtro temporal| FILTER
    LS --> LR
    LR --> DB

    U -->|inicia checkout| PC
    PC --> PS
    PS --> SS
    SS --> STRIPE
    STRIPE -->|webhook de checkout| SC
    SC --> WH
    WH -->|cria locação| LS
    U -->|atualiza status| LC
    LC --> LS
    LS -->|publica após salvar| EVENT
    EVENT --> LISTENER
    LISTENER --> MSG
    MSG --> MR
    MR --> DB
    WH --> US
    US --> UR
    UR --> DB
    US --> MS
    MS --> MAIL
```

Na consulta, `LeasingState` escolhe qual dos três parâmetros temporais é preenchido antes da chamada a `LeasingRepository`. Na criação, o cliente abre o checkout, a Stripe devolve o resultado por webhook, `StripeWebhookService` chama `LeasingService.createLeasing` e a locação é salva. O evento de mudança aciona a notificação de forma síncrona. O serviço de e-mail aparece como integração do fluxo de contas de usuário, não como parte da notificação de locação.
