# LootSafe v2

> Status: em desenvolvimento (fase de desenvolvimento e cobertura de testes).

Reescrita do projeto [LootSafe](https://github.com/KaykMurphy/lootSafe) com foco em código mais limpo, melhor organização e evolução incremental do domínio de escrow digital.

Assim como a versão original, a API intermedia transações digitais com pagamento via Pix, mantendo o valor retido até a liberação do produto, tratando disputas por mediação e recebendo notificações de webhook do Mercado Pago.

## Diferenças em relação ao LootSafe original

- Java 21 em vez de Java 17.
- Spring Boot 4.1.0.
- PostgreSQL como banco padrão via Docker Compose (porta `5435`), com H2 disponível para testes.
- Migrações de banco reais com **Flyway** (`V1__initial_schema.sql` em vez de `ddl-auto`).
- `spring.jpa.hibernate.ddl-auto=validate` para garantir que o schema do banco esteja sempre alinhado com as entidades.
- Auditoria JPA centralizada em `AbstractAuditableEntity` (`id` UUID, `created_at`, `updated_at`).
- Autenticação com **JWT** (OAuth2 Resource Server) + **refresh token rotativo**.
- Integração real com o **SDK do Mercado Pago** para geração de cobranças Pix e conciliação via webhooks.
- Domínio em oito entidades: `User`, `Announcement`, `Transaction`, `DisputeChat`, `DisputeMessage`, `Payment`, `RefreshToken` e `PaymentWebhookEvent`.
- Chat dentro das disputas com envio e listagem de mensagens por participantes ou admin.
- Expiração automática de cobranças Pix pendentes via scheduler.
- Webhooks do Mercado Pago persistidos e **deduplicados** (`PaymentWebhookEvent`) para rastreabilidade, com reprocessamento automático de eventos com falha via scheduler.
- **Segurança Avançada:** Rate Limiting (Bucket4j), Sanitização de XSS, Política de senhas fortes, Endpoint de Logout com blacklist de tokens e Headers de Proxy Reverso configurados.
- **Resiliência:** Lock distribuído com **ShedLock** e health checks de monitoramento ativos (**Actuator**).
- **Infraestrutura:** Docker Multi-stage build (Temurin 21) e docker-compose.yml pronto para produção (validação *Fail-Fast*).
- Confirmação de recebimento pelo comprador (liberação do valor no escrow) e cancelamento de reserva pelo vendedor.
- Operações administrativas além de listagem: cancelar transação, reembolsar transação/pagamento e cancelar pagamento.
- Endpoints administrativos protegidos para o papel `ADMIN`; acesso a usuário e transação restrito ao dono ou admin.

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- Spring Security + OAuth2 Resource Server (JWT)
- Spring Validation
- Spring Boot Actuator
- Bucket4j (Rate Limiting)
- ShedLock (Distributed Locks)
- Flyway
- PostgreSQL
- H2 (testes)
- Mercado Pago SDK (`com.mercadopago:sdk-java`)
- MapStruct 1.6.3
- Lombok
- Docker & Docker Compose
- Maven Wrapper

## Estrutura do Projeto

```text
src/main/java/com/lootsafe
|-- config        # Segurança, JWT, Mercado Pago SDK, async, scheduling e auditoria JPA
|-- controller    # REST controllers (users, announcements, transactions, disputes, admin)
|-- dto           # Contratos de request/response
|-- entity        # Entidades JPA (User, Announcement, Transaction, DisputeChat, DisputeMessage, Payment, RefreshToken, PaymentWebhookEvent)
|-- enums         # UserRole, TransactionStatus, AnnouncementStatus, DisputeStatus, PaymentStatus, PaymentProvider, WebhookEventStatus
|-- exception     # Exceções de domínio + GlobalExceptionHandler (@RestControllerAdvice)
|-- mapper        # MapStruct mappers de entidade para DTO
|-- payment       # Integração com Mercado Pago (client, serviços de pagamento, processamento e webhook)
|-- repository    # Repositórios Spring Data JPA
|-- security      # Encriptação AES/GCM e conversor de JWT
|-- scheduler     # Agendamentos (expiração de cobranças Pix e retry de webhooks)
|-- service       # Camada de serviços de negócio
|-- swagger       # OpenAPI com autorização Bearer
`-- resources/db/migration   # Scripts Flyway
```

## Fluxo de pagamento

1. O comprador inicia uma transação pelo token do anúncio (`POST /api/transactions`).
2. O serviço cria uma ordem **Pix** no Mercado Pago (24h de validade) e grava o `Payment` no estado `PENDING`; o anúncio passa para `RESERVED`.
3. O Mercado Pago notifica `POST /api/webhooks/mercadopago` (assinatura validada via `x-signature`).
4. O webhook é persistido (deduplicado por `external_event_id`) e concilia a ordem, confirma o pagamento, aprova a transação, marca o anúncio como `SOLD` e grava `paid_at`. Eventos com falha são reprocessados automaticamente.
5. O comprador acessa `GET /api/transactions/{id}/credentials` e recebe as credenciais do produto descriptografadas.
6. O comprador confirma o recebimento (`POST /api/transactions/{id}/confirm`) e o valor é liberado (transação `RELEASED`).

Cobranças Pix pendentes que passam do prazo de validade são canceladas pelo scheduler (`PaymentScheduler`), a transação é cancelada e o anúncio volta a ficar ativo (`ACTIVE`). O intervalo de checagem é configurado por `payment.expiration-check-interval-ms` (padrão `3600000` ms = 1h). O retry de webhooks com falha roda em `payment.webhook-retry-interval-ms` (padrão `300000` ms = 5 min).

## API

### Autenticação (`/api/users`)

| Método | Rota       | Descrição                          | Acesso |
| ------ | ---------- | ---------------------------------- | ------ |
| POST   | `/register`| Cria usuário e emite tokens        | Público |
| POST   | `/login`   | Login com email/senha              | Público |
| POST   | `/refresh` | Rotaciona o refresh token          | Público |
| GET    | `/{id}`    | Busca usuário (dono ou admin)      | Autenticado |
| PUT    | `/{id}`    | Atualiza nome e pix key            | Autenticado |

### Anúncios (`/api/announcements`)

| Método | Rota                 | Descrição                              | Acesso |
| ------ | -------------------- | -------------------------------------- | ------ |
| POST   | `/`                  | Cria anúncio (encripta credenciais)    | SELLER |
| GET    | `/{token}`           | Busca anúncio por token público        | Público |
| PUT    | `/{id}`              | Atualiza anúncio (dono)                | SELLER |
| DELETE | `/{id}`              | Cancela anúncio (dono)                 | SELLER |
| POST   | `/{id}/cancel-reservation` | Cancela reserva e transação pendente | SELLER |

### Transações (`/api/transactions`)

| Método | Rota                 | Descrição                                       | Acesso |
| ------ | -------------------- | ----------------------------------------------- | ------ |
| POST   | `/`                  | Inicia transação e cria cobrança Pix            | BUYER |
| GET    | `/{id}`              | Busca transação com pagamento (participante/admin) | Autenticado |
| GET    | `/{id}/credentials`  | Libera credenciais do produto após pagamento    | BUYER |
| POST   | `/{id}/confirm`      | Confirma recebimento e libera o valor no escrow | BUYER |

### Disputas (`/api/disputes`)

| Método | Rota            | Descrição                              | Acesso |
| ------ | --------------- | -------------------------------------- | ------ |
| POST   | `/`             | Abre disputa (comprador/vendedor)      | Autenticado |
| PUT    | `/{id}/resolve` | Resolve disputa (release ou refund)    | ADMIN |

### Mensagens de disputa (`/api/disputes/{disputeId}/messages`)

| Método | Rota | Descrição                       | Acesso |
| ------ | ---- | ------------------------------- | ------ |
| POST   | `/`  | Envia mensagem na disputa       | Participante / ADMIN |
| GET    | `/`  | Lista mensagens da disputa      | Participante / ADMIN |

### Administração (`/api/admin`)

| Método | Rota                            | Descrição                                   | Acesso |
| ------ | ------------------------------- | ------------------------------------------- | ------ |
| GET    | `/users`                        | Lista usuários                              | ADMIN |
| GET    | `/transactions`                 | Lista transações (filtro por status)        | ADMIN |
| GET    | `/disputes`                     | Lista disputas                              | ADMIN |
| GET    | `/payments`                     | Lista pagamentos (filtro por status)        | ADMIN |
| POST   | `/transactions/{id}/cancel`     | Cancela transação pendente                  | ADMIN |
| POST   | `/transactions/{id}/refund`     | Reembolsa transação aprovada/em disputa     | ADMIN |
| POST   | `/payments/{id}/cancel`         | Cancela pagamento                           | ADMIN |

### Webhooks (`/api/webhooks`)

| Método | Rota            | Descrição                                      | Acesso |
| ------ | --------------- | ---------------------------------------------- | ------ |
| POST   | `/mercadopago`  | Recebe notificações de ordem do Mercado Pago   | Público |

Documentação interativa em `/swagger-ui.html` quando o profile `dev` estiver ativo.

## Estados do domínio

- **Anúncio**: `DRAFT` → `ACTIVE` → `RESERVED` → `SOLD` / `CANCELLED`
- **Transação**: `PENDING` → `APPROVED` → `DISPUTED` → `RELEASED` / `REFUNDED` / `CANCELLED`
- **Pagamento**: `PENDING` → `APPROVED` / `REJECTED` / `CANCELLED` / `REFUNDED` / `EXPIRED`
- **Evento de webhook**: `RECEIVED` → `PROCESSED` / `FAILED`

As transições são encapsuladas em métodos de domínio nas entidades (ex.: `announcement.reserve()`, `transaction.approve()`), validando o estado atual antes de avançar.

## Pré-requisitos

- Java 21
- Docker (para subir o PostgreSQL local)

## Como executar

**Para Produção (via Docker Compose):**
É necessário configurar o seu `.env` com todas as chaves (consulte a raiz do projeto).
```bash
docker compose up -d --build
```

**Para Desenvolvimento Local:**
```bash
# 1. Suba o banco de desenvolvimento (PostgreSQL na porta 5435)
docker compose -f docker-compose-dev.yml up -d

# 2. Rode a aplicação com o profile dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No profile `dev`, o Flyway aplica as migrações automaticamente ao subir a aplicação e o Hibernate valida o schema com `ddl-auto=validate`.

## Configuração

As credenciais do banco local estão em `application-dev.properties` (usuário/senha `escrow`). Para produção, as credenciais são injetadas via variáveis de ambiente e o arquivo `.env` é ignorado pelo Git (validação com sintaxe Fail-Fast no `docker-compose.yml`).

### Segurança (JWT, XSS e Rate Limit)

No profile `dev`, `jwt.secret` e expirações têm valores padrão. Em produção, defina via variáveis de ambiente. Todas as requisições públicas sensíveis (como Login e Webhooks) possuem **Rate Limiting** via Bucket4j, e os DTOs bloqueiam caracteres nocivos de injeção XSS nas Strings.

### Encriptação

As credenciais dos anúncios são encriptadas com AES/GCM via `EncryptionConfig`. No profile `dev` há valores padrão (`encryption.password=dev-password` e `encryption.salt=deadbeefdeadbeef`). Em produção, defina via `.env`.

### Mercado Pago

As credenciais do SDK são configuradas por `mercadopago.access-token` e `mercadopago.webhook-secret`.

### Schedulers (Expiração e Retrys)

Os agendamentos (Scheduler de pagamentos Pix e Retry de Webhooks com falha) usam **ShedLock** para evitar que múltiplas instâncias rodem o mesmo script em concorrência, travando o banco por 10 minutos ou conforme configurado no banco.

### Administração

Os endpoints `/api/admin` exigem o papel `ADMIN`. Em `dev`, promova um usuário a `ADMIN` diretamente no banco para testar os endpoints.

## Próximos Passos

- **Testes automatizados:** Em andamento (cobertura de testes unitários de domínio e serviços, além de integração com MockMvc).
- Cancelamento de reservas quando o anúncio for excluído.
- Notificações por e-mail de eventos de pagamento e disputa.
- Paginação e filtros nos endpoints de listagem.

## Licença

Projeto de estudo pessoal.
