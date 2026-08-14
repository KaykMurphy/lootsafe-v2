# LootSafe v2

> Status: em desenvolvimento.

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
- Domínio em sete entidades: `User`, `Announcement`, `Transaction`, `DisputeChat`, `DisputeMessage`, `Payment` e `RefreshToken`.
- Chat dentro das disputas com envio e listagem de mensagens por participantes ou admin.
- Expiração automática de cobranças Pix pendentes via scheduler.
- Endpoints administrativos de listagem protegidos para o papel `ADMIN`.

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- Spring Security + OAuth2 Resource Server (JWT)
- Spring Validation
- Spring Boot Actuator
- Flyway
- PostgreSQL
- H2 (testes)
- Mercado Pago SDK (`com.mercadopago:sdk-java`)
- MapStruct 1.6.3
- Lombok
- Maven Wrapper

## Estrutura do Projeto

```text
src/main/java/com/lootsafe
|-- config        # Segurança, JWT, Mercado Pago SDK, async, scheduling e auditoria JPA
|-- controller    # REST controllers (users, announcements, transactions, disputes, admin)
|-- dto           # Contratos de request/response
|-- entity        # Entidades JPA (User, Announcement, Transaction, DisputeChat, DisputeMessage, Payment, RefreshToken)
|-- enums         # UserRole, TransactionStatus, AnnouncementStatus, DisputeStatus, PaymentStatus, PaymentProvider
|-- exception     # Exceções de domínio + GlobalExceptionHandler (@RestControllerAdvice)
|-- mapper        # MapStruct mappers de entidade para DTO
|-- payment       # Integração com Mercado Pago (client, serviços de pagamento e webhook)
|-- repository    # Repositórios Spring Data JPA
|-- security      # Encriptação AES/GCM e conversor de JWT
|-- scheduler     # Agendamentos (expiração de cobranças Pix)
|-- service       # Camada de serviços de negócio
|-- swagger       # OpenAPI com autorização Bearer
`-- resources/db/migration   # Scripts Flyway
```

## Fluxo de pagamento

1. O comprador inicia uma transação pelo token do anúncio (`POST /api/transactions`).
2. O serviço cria uma ordem **Pix** no Mercado Pago (24h de validade) e grava o `Payment` no estado `PENDING`; o anúncio passa para `RESERVED`.
3. O Mercado Pago notifica `POST /api/webhooks/mercadopago` (assinatura validada via `x-signature`).
4. O webhook concilia a ordem, confirma o pagamento, aprova a transação, marca o anúncio como `SOLD` e grava `paid_at`.
5. O comprador acessa `GET /api/transactions/{id}/credentials` e recebe as credenciais do produto descriptografadas.

Cobranças Pix pendentes que passam do prazo de validade são canceladas pelo scheduler (`PaymentScheduler`), a transação é cancelada e o anúncio volta a ficar ativo (`ACTIVE`). O intervalo de checagem é configurado por `payment.expiration-check-interval-ms` (padrão `3600000` ms = 1h).

## API

### Autenticação (`/api/users`)

| Método | Rota        | Descrição                          | Acesso |
| ------ | ----------- | ---------------------------------- | ------ |
| POST   | `/register` | Cria usuário e emite tokens        | Público |
| POST   | `/login`    | Login com email/senha              | Público |
| POST   | `/refresh`  | Rotaciona o refresh token          | Público |
| GET    | `/{id}`     | Busca usuário                      | Autenticado |
| PUT    | `/{id}`     | Atualiza usuário                   | Autenticado |

### Anúncios (`/api/announcements`)

| Método | Rota       | Descrição                              | Acesso |
| ------ | ---------- | -------------------------------------- | ------ |
| POST   | `/`        | Cria anúncio (encripta credenciais)    | SELLER |
| GET    | `/{token}` | Busca anúncio por token público        | Público |
| PUT    | `/{id}`    | Atualiza anúncio (dono)                | Autenticado |
| DELETE | `/{id}`    | Cancela anúncio (dono)                 | Autenticado |

### Transações (`/api/transactions`)

| Método | Rota                 | Descrição                                       | Acesso |
| ------ | -------------------- | ----------------------------------------------- | ------ |
| POST   | `/`                  | Inicia transação e cria cobrança Pix            | BUYER |
| GET    | `/{id}`              | Busca transação com pagamento                    | Autenticado |
| GET    | `/{id}/credentials`  | Libera credenciais do produto após pagamento    | BUYER |

### Disputas (`/api/disputes`)

| Método | Rota            | Descrição                              | Acesso |
| ------ | --------------- | -------------------------------------- | ------ |
| POST   | `/`             | Abre disputa (comprador/vendedor)      | Autenticado |
| PUT    | `/{id}/resolve` | Resolve disputa (release ou refund)    | Autenticado |

### Mensagens de disputa (`/api/disputes/{disputeId}/messages`)

| Método | Rota | Descrição                       | Acesso |
| ------ | ---- | ------------------------------- | ------ |
| POST   | `/`  | Envia mensagem na disputa       | Participante / ADMIN |
| GET    | `/`  | Lista mensagens da disputa      | Participante / ADMIN |

### Administração (`/api/admin`)

| Método | Rota             | Descrição                                   | Acesso |
| ------ | ---------------- | ------------------------------------------- | ------ |
| GET    | `/users`         | Lista usuários                              | ADMIN |
| GET    | `/transactions`  | Lista transações (filtro por status)        | ADMIN |
| GET    | `/disputes`      | Lista disputas                              | ADMIN |
| GET    | `/payments`      | Lista pagamentos (filtro por status)        | ADMIN |

### Webhooks (`/api/webhooks`)

| Método | Rota            | Descrição                                      | Acesso |
| ------ | --------------- | ---------------------------------------------- | ------ |
| POST   | `/mercadopago`  | Recebe notificações de ordem do Mercado Pago   | Público |

Documentação interativa em `/swagger-ui.html` quando o profile `dev` estiver ativo.

## Estados do domínio

- **Anúncio**: `DRAFT` → `ACTIVE` → `RESERVED` → `SOLD` / `CANCELLED`
- **Transação**: `PENDING` → `APPROVED` → `DISPUTED` → `RELEASED` / `REFUNDED`
- **Pagamento**: `PENDING` → `APPROVED` / `REJECTED` / `CANCELLED` / `REFUNDED` / `EXPIRED`

As transições são encapsuladas em métodos de domínio nas entidades (ex.: `announcement.reserve()`, `transaction.approve()`), validando o estado atual antes de avançar.

## Pré-requisitos

- Java 21
- Docker (para subir o PostgreSQL local)

## Como executar

```bash
# 1. Suba o banco de desenvolvimento (PostgreSQL na porta 5435)
docker compose -f docker-compose-dev.yml up -d

# 2. Rode a aplicação com o profile dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No profile `dev`, o Flyway aplica as migrações automaticamente ao subir a aplicação e o Hibernate valida o schema com `ddl-auto=validate`.

## Configuração

As credenciais do banco local estão em `application-dev.properties` (usuário/senha `escrow`). Para produção, as credenciais são injetadas via variáveis de ambiente e o arquivo `.env` é ignorado pelo Git.

### JWT

No profile `dev`, `jwt.secret`, `jwt.expirationMs` (15 min) e `jwt.refreshExpirationMs` (7 dias) têm valores padrão. Em produção, defina via variáveis de ambiente.

### Encriptação

As credenciais dos anúncios são encriptadas com AES/GCM via `EncryptionConfig`. No profile `dev` há valores padrão (`encryption.password=dev-password` e `encryption.salt=deadbeefdeadbeef`). Em produção, defina `encryption.password` e `encryption.salt` (salt em hexadecimal de 16 bytes) via variáveis de ambiente.

### Mercado Pago

As credenciais do SDK são configuradas por `mercadopago.access-token` e `mercadopago.webhook-secret`. No profile `dev` há valores temporários; em produção, defina as variáveis `MERCADOPAGO_ACCESS_TOKEN` e `MERCADOPAGO_WEBHOOK_SECRET`.

### Expiração de pagamentos

O scheduler de expiração lê `payment.expiration-check-interval-ms` (env: `PAYMENT_EXPIRATION_CHECK_INTERVAL_MS`). No profile `dev` o padrão é `3600000` (1h); em produção, defina via variável de ambiente.

### Administração

Os endpoints `/api/admin` exigem o papel `ADMIN`. Em `dev`, os usuários são criados com os papéis `BUYER` e `SELLER`; promova um usuário a `ADMIN` diretamente no banco para testar os endpoints.

## Próximos Passos

- Cancelamento de reservas quando o anúncio for excluído.
- Notificações por e-mail de eventos de pagamento e disputa.
- Paginação e filtros nos endpoints de listagem.
- Testes automatizados de integração.

## Licença

Projeto de estudo pessoal.
