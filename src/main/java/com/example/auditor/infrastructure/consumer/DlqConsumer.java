package com.example.auditor.infrastructure.consumer;

import com.example.auditor.application.ports.in.service.AuditServicePort;
import com.example.auditor.infrastructure.config.DlqProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final SqsClient sqsClient;
    private final AuditServicePort auditService;
    private final DlqProperties properties;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DlqConsumer(SqsClient sqsClient, AuditServicePort auditService, DlqProperties properties) {
        this.sqsClient = sqsClient;
        this.auditService = auditService;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        executorService.submit(this::pollLoop);
    }

    private void pollLoop() {
        String queueUrl = sqsClient.getQueueUrl(builder -> builder.queueName(properties.getEntradaPedido())).queueUrl();
        log.info("Consumindo DLQ: {}", properties.getEntradaPedido());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20)
                        .build());

                response.messages().forEach(message -> {
                    try {
                        auditService.createFromDlq(message.body(), properties.getEntradaPedido());

                        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());

                        log.info("Mensagem persistida e removida da DLQ com sucesso");
                    } catch (Exception exception) {
                        log.error("Falha ao persistir mensagem da DLQ. Ela permanecerá na fila.", exception);
                    }
                });
            } catch (Exception exception) {
                log.error("Erro no polling da DLQ", exception);
            }
        }
    }
}
