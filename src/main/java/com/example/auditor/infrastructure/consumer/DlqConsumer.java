package com.example.auditor.infrastructure.consumer;

import com.example.auditor.application.ports.in.service.AuditServicePort;
import com.example.auditor.infrastructure.config.DlqProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Adapter de entrada: faz long polling na DLQ e entrega cada mensagem ao serviço de auditoria.
 * A mensagem só é apagada da fila depois de persistida; se algo falhar, ela continua na DLQ.
 *
 * Pode ser desligado com {@code auditor.consumer.enabled=false} (usado nos testes, que não têm AWS).
 */
@Component
@ConditionalOnProperty(prefix = "auditor.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20;
    private static final long RETRY_DELAY_MILLIS = 5_000;

    private final SqsClient sqsClient;
    private final AuditServicePort auditService;
    private final DlqProperties properties;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dlq-consumer");
        thread.setDaemon(true);
        return thread;
    });

    public DlqConsumer(SqsClient sqsClient, AuditServicePort auditService, DlqProperties properties) {
        this.sqsClient = sqsClient;
        this.auditService = auditService;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        executorService.submit(this::pollLoop);
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(WAIT_TIME_SECONDS + 5L, TimeUnit.SECONDS)) {
            log.warn("Consumer da DLQ não terminou dentro do prazo de encerramento");
        }
    }

    private void pollLoop() {
        String queueName = properties.getEntradaPedido();
        String queueUrl = resolveQueueUrl(queueName);
        if (queueUrl == null) {
            return;
        }
        log.info("Consumindo DLQ: {}", queueName);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(MAX_MESSAGES)
                        .waitTimeSeconds(WAIT_TIME_SECONDS)
                        .build());

                response.messages().forEach(message -> process(message, queueUrl, queueName));
            } catch (Exception exception) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.error("Erro no polling da DLQ; nova tentativa em {} ms", RETRY_DELAY_MILLIS, exception);
                pause();
            }
        }
        log.info("Consumer da DLQ encerrado");
    }

    private void process(Message message, String queueUrl, String queueName) {
        try {
            auditService.createFromDlq(message.body(), queueName);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            log.info("Mensagem {} persistida e removida da DLQ", message.messageId());
        } catch (Exception exception) {
            log.error("Falha ao auditar a mensagem {}. Ela permanecerá na fila.", message.messageId(), exception);
        }
    }

    /** Tenta descobrir a URL da fila até conseguir ou até o serviço ser encerrado. */
    private String resolveQueueUrl(String queueName) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                return sqsClient.getQueueUrl(builder -> builder.queueName(queueName)).queueUrl();
            } catch (Exception exception) {
                log.error("Não foi possível obter a URL da fila {}; nova tentativa em {} ms",
                        queueName, RETRY_DELAY_MILLIS, exception);
                pause();
            }
        }
        return null;
    }

    private void pause() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
