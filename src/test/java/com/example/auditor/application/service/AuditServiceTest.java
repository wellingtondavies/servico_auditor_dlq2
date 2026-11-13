package com.example.auditor.application.service;

import com.example.auditor.application.ports.out.ErrorRecordRepositoryPort;
import com.example.auditor.core.domain.ErrorRecord;
import com.example.auditor.core.domain.Severity;
import com.example.auditor.core.domain.Status;
import com.example.auditor.mapper.InvalidPayloadException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o núcleo sem Spring nem AWS: a porta de saída é trocada por um repositório em memória,
 * que é justamente o que a arquitetura hexagonal permite.
 */
class AuditServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T12:00:00Z");
    private static final String FILA = "fila-teste-DLQ.fifo";

    private final InMemoryRepository repository = new InMemoryRepository();
    private final AuditService service =
            new AuditService(repository, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void criaRegistroPendenteComDadosDaMensagem() {
        String payload = pedido(10, 5);

        ErrorRecord record = service.createFromDlq(payload, FILA);

        assertNotNull(record.getErrorId());
        assertEquals(FILA, record.getQueueName());
        assertEquals(payload, record.getPayload());
        assertEquals(AGORA, record.getTimestamp());
        assertEquals(Status.PENDING_ANALYSIS, record.getStatus());
        assertEquals(Severity.LOW, record.getSeverity());
        assertEquals(1, repository.salvos.size());
    }

    @Test
    void somaAsQuantidadesDeTodosOsItensParaDefinirGravidade() {
        assertEquals(Severity.MEDIUM, service.createFromDlq(pedido(30, 20), FILA).getSeverity());
        assertEquals(Severity.HIGH, service.createFromDlq(pedido(60, 41), FILA).getSeverity());
    }

    @Test
    void pedidoSemItensOuComQuantidadeNulaContaComoZero() {
        assertEquals(Severity.LOW, service.createFromDlq("{\"customerId\":1}", FILA).getSeverity());
        assertEquals(Severity.LOW, service.createFromDlq(
                "{\"orderItems\":[{\"sku\":1,\"amount\":null}]}", FILA).getSeverity());
    }

    @Test
    void camposDesconhecidosNoPayloadNaoImpedemAuditoria() {
        String payload = "{\"orderItems\":[{\"sku\":1,\"amount\":60}],\"campoNovo\":true}";

        assertEquals(Severity.MEDIUM, service.createFromDlq(payload, FILA).getSeverity());
    }

    @Test
    void payloadInvalidoLancaExcecaoENaoPersiste() {
        assertThrows(InvalidPayloadException.class, () -> service.createFromDlq("nao é json", FILA));
        assertThrows(InvalidPayloadException.class, () -> service.createFromDlq("null", FILA));
        assertThrows(InvalidPayloadException.class, () -> service.createFromDlq("", FILA));
        assertTrue(repository.salvos.isEmpty());
    }

    private static String pedido(int... quantidades) {
        StringBuilder itens = new StringBuilder();
        for (int i = 0; i < quantidades.length; i++) {
            if (i > 0) {
                itens.append(',');
            }
            itens.append("{\"sku\":").append(i + 1).append(",\"amount\":").append(quantidades[i]).append('}');
        }
        return "{\"zipCode\":\"85840-000\",\"customerId\":7,\"orderItems\":[" + itens
                + "],\"origin\":\"site\",\"occurredAt\":\"2026-01-01T11:59:00Z\"}";
    }

    private static final class InMemoryRepository implements ErrorRecordRepositoryPort {
        private final List<ErrorRecord> salvos = new ArrayList<>();

        @Override
        public ErrorRecord save(ErrorRecord record) {
            salvos.add(record);
            return record;
        }
    }
}
