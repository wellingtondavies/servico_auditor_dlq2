Resumo

Objetivo: Consumir mensagens da DLQ, triar severidade e persistir registros de auditoria para análise posterior.
Responsabilidade única: Serviço independente dedicado apenas à auditoria de mensagens com falha.
Arquitetura Escolhida

Padrão: Arquitetura Hexagonal (Ports & Adapters).
Motivo: Isola regras de negócio das dependências externas (SQS, JPA), deixando o núcleo do domínio testável e estável. Facilita a evolução do serviço sem impactar a lógica de triagem nem a forma de persistência.
Mapeamento para o Código

Domínio: ErrorRecord, Severity, Status — ErrorRecord.java
Aplicação / Serviço de Domínio: AuditService, implementa a triagem de severidade e cria o ErrorRecord, AuditService.java
Ports (Interfaces): contratos de entrada/saída, AuditServicePort.java e ErrorRecordRepositoryPort.java
Adapters / Infraestrutura: consumer SQS (DlqConsumer), configuração SQS (DlqProperties, SqsConfig) e persistência JPA (ErrorRecordEntity, ErrorRecordRepositoryAdapter, ErrorRecordJpaRepository),
Consumer: DlqConsumer.java
Config: DlqProperties.java e SqsConfig.java
Persistência: ErrorRecordEntity.java e ErrorRecordRepositoryAdapter.java
Fluxo de Processamento (visão prática)

Recebe: consumer faz polling na DLQ configurada e obtém a mensagem (DlqConsumer).
Triagem: AuditService converte payload, soma orderItems e determina Severity (HIGH / MEDIUM / LOW) conforme regra de negócio.
Constrói registro: cria ErrorRecord com errorId, queueName, payload, timestamp, status e severity.
Persiste: salva via adapter JPA (ErrorRecordRepositoryAdapter → ErrorRecordJpaRepository).
Confirmação: remoção da mensagem da DLQ só após persistência bem-sucedida, garantindo durabilidade.
Por que essa arquitetura funciona para este serviço

Separa preocupações: regras de negócio (triagem) ficam no núcleo; integrações externas ficam em adapters. Essa separação reduz acoplamento e facilita revisão de falhas sem tocar infra.
Testabilidade: com ports bem definidos, é trivial mockar adaptadores em testes de unidade para validar apenas a lógica de severidade e criação de registros.
Evolução e manutenção: novas fontes de mensagens ou alterações no banco podem ser implementadas trocando/adicionando adapters, sem alterar o núcleo do domínio.
Clareza de responsabilidade: o serviço tem uma única responsabilidade (auditoria), e a arquitetura hexagonal reforça esse contrato, simplificando entendimento, auditoria e governança do código.
Contrato de Persistência (exemplo)

O registro armazenado contém: errorId, queueName, payload, timestamp, status, severity, conforme a entidade JPA em ErrorRecordEntity.java