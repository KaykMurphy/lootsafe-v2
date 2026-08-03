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
- Domínio inicial em quatro entidades: `User`, `Announcement`, `Transaction` e `DisputeChat`.

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Boot Actuator
- Flyway
- PostgreSQL
- H2 (testes)
- MapStruct 1.6.3
- Lombok
- Maven Wrapper

## Estrutura do Projeto

```text
src/main/java/com/lootsafe
|-- config        # Auditoria JPA
|-- entity        # Entidades JPA (User, Announcement, Transaction, DisputeChat)
|-- enums         # UserRole, TransactionStatus, AnnouncementStatus, DisputeStatus
|-- exception     # Exceções de domínio (Business, ResourceNotFound, Unauthorized, Encryption)
|-- repository    # Repositórios Spring Data JPA
|-- security      # Configuração de encriptação (AES/GCM)
|-- service       # Camada de serviços de negócio
`-- resources/db/migration   # Scripts Flyway
```

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

## Variáveis de Ambiente

As credenciais do banco local estão em `application-dev.properties` (usuário/senha `escrow`). Para produção, as credenciais são injetadas via variáveis de ambiente e o arquivo `.env` é ignorado pelo Git (`gitignore`).

As credenciais dos anúncios são encriptadas com AES/GCM via `EncryptionConfig`. As variáveis `ENCRYPTION_PASSWORD` e `ENCRYPTION_SALT` (salt em hexadecimal de 16 bytes) devem estar disponíveis no ambiente de execução.

## Próximos Passos

- Camada de controllers (REST API).
- Tratamento global de exceções (`@RestControllerAdvice`) mapeando as exceções de domínio para HTTP.
- Integração com Mercado Pago (geração de Pix e webhooks).
- Fluxo de mediação e chat de disputas.

## Licença

Projeto de estudo pessoal.
