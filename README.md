# Serviço auditor de falhas (DLQ)

Microsserviço que consome a fila de mensagens mortas (DLQ) dos pedidos no Amazon SQS, classifica a
gravidade de cada falha pela quantidade de itens do pedido e grava um registro de auditoria para
análise posterior. Tem uma única responsabilidade: auditar mensagens que falharam.

Atividade 5 — Implementando um Serviço Auditor de Falhas (DLQ). Projeto acadêmico de Engenharia
de Software — Centro Universitário FAG.

**Autor:** Wellington Davies dos Santos.

## Arquitetura

Arquitetura hexagonal (Ports & Adapters): a regra de negócio não conhece SQS nem JPA, só as portas.

```
                  ┌──────────────────────── núcleo ────────────────────────┐
 Amazon SQS       │  porta de entrada        serviço           domínio      │      H2 (JPA)
 DLQ FIFO  ─────► │  AuditServicePort  ──►  AuditService  ──►  ErrorRecord  │ ───► error_records
 DlqConsumer      │                          │                 Severity     │      Adapter JPA
 (adapter)        │                          └──► ErrorRecordRepositoryPort │
                  └─────────────────────────────────────────────────────────┘
```

| Camada                  | Elementos                                                                     |
| ----------------------- | ----------------------------------------------------------------------------- |
| Domínio                 | `ErrorRecord`, `Severity` (com a regra de gravidade), `Status`                |
| Aplicação               | `AuditService`: converte o payload, soma os itens e cria o registro           |
| Portas                  | `AuditServicePort` (entrada) e `ErrorRecordRepositoryPort` (saída)            |
| Adapter de entrada      | `DlqConsumer`: long polling no SQS                                            |
| Adapter de saída        | `ErrorRecordRepositoryAdapter`, `ErrorRecordEntity`, `ErrorRecordJpaRepository` |
| Configuração            | `SqsConfig` (cliente SQS) e `DlqProperties` (fila e região)                   |

### Por que hexagonal aqui

- **Separação:** a triagem fica no núcleo; trocar SQS por Kafka ou H2 por PostgreSQL é trocar um
  adapter, sem tocar na regra.
- **Testabilidade:** `AuditServiceTest` roda sem Spring e sem AWS, trocando o repositório por uma
  implementação em memória.
- **Clareza:** um serviço, uma responsabilidade, com o contrato explícito nas portas.

## Fluxo de processamento

1. O `DlqConsumer` faz long polling (até 10 mensagens, espera de 20 s) na DLQ configurada.
2. O `AuditService` converte o JSON, soma `amount` de todos os `orderItems` e pede a gravidade ao
   domínio.
3. É criado um `ErrorRecord` com status `PENDING_ANALYSIS` e salvo pelo adapter JPA.
4. Só depois de salvo a mensagem é apagada da DLQ. Se qualquer passo falhar, ela continua na fila.

## Regra de gravidade

| Total de itens no pedido | Gravidade |
| ------------------------ | --------- |
| até 49                   | `LOW`     |
| de 50 a 100              | `MEDIUM`  |
| acima de 100             | `HIGH`    |

## Estrutura do repositório

```
src/main/java/com/example/auditor/
├── AuditorApplication.java
├── core/domain/                       ErrorRecord, Severity, Status
├── application/
│   ├── ports/in/service/              AuditServicePort
│   ├── ports/out/                     ErrorRecordRepositoryPort
│   └── service/                       AuditService
├── infrastructure/
│   ├── consumer/                      DlqConsumer
│   ├── config/                        SqsConfig, DlqProperties
│   └── out/persistence/               entidade, repositório JPA e adapter
├── dto/                               OrderEventDTO, OrderItemDTO
└── mapper/                            OrderEventMapper, InvalidPayloadException
src/test/java/com/example/auditor/     testes do domínio, do serviço e do contexto
```

## Como executar

Requisitos: JDK 17 e acesso a uma conta AWS com a DLQ criada.

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
./mvnw spring-boot:run           # no Windows: mvnw.cmd spring-boot:run
```

Testes (não precisam de AWS):

```bash
./mvnw test
```

Com o serviço rodando, os registros podem ser vistos no console do H2 em
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:auditor`, usuário `sa`, sem senha).

## Configuração

Nenhuma credencial é versionada. O SDK da AWS lê as credenciais da cadeia padrão: variáveis de
ambiente, `~/.aws/credentials` ou o perfil da instância.

| Variável                    | Padrão                                      | Função                       |
| --------------------------- | ------------------------------------------- | ---------------------------- |
| `AWS_ACCESS_KEY_ID`         | —                                           | Credencial AWS               |
| `AWS_SECRET_ACCESS_KEY`     | —                                           | Credencial AWS               |
| `AWS_REGION`                | `us-east-1`                                 | Região da fila               |
| `DLQ_QUEUE_NAME`            | `T02N_WELLINGTON_DAVIES_DOS_SANTOS-DLQ.fifo` | DLQ consumida               |
| `AUDITOR_CONSUMER_ENABLED`  | `true`                                      | Liga o polling da DLQ        |

## Contrato

Mensagem esperada na DLQ:

```json
{"zipCode":"85840-000","customerId":7,"orderItems":[{"sku":1,"amount":60}],
 "origin":"site","occurredAt":"2026-01-01T11:59:00Z"}
```

Registro gravado na tabela `error_records`: `error_id`, `queue_name`, `payload`, `timestamp`,
`status` e `severity`.

## Limitações

- H2 em memória: os registros somem quando o serviço para. Em produção, trocar o adapter de saída
  por um banco persistente.
- Uma mensagem que nunca pode ser processada (JSON inválido) continua na DLQ e é lida de novo a
  cada ciclo; não há uma fila de descarte para ela.
- O status `ANALYZED`/`IGNORED` existe no domínio, mas ainda não há endpoint para mudar o status
  de um registro.
