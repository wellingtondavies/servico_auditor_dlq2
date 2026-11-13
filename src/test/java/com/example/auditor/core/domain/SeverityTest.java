package com.example.auditor.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeverityTest {

    @Test
    void totalAbaixoDe50EhBaixa() {
        assertEquals(Severity.LOW, Severity.fromTotalItems(0));
        assertEquals(Severity.LOW, Severity.fromTotalItems(49));
    }

    @Test
    void totalEntre50e100EhMedia() {
        assertEquals(Severity.MEDIUM, Severity.fromTotalItems(50));
        assertEquals(Severity.MEDIUM, Severity.fromTotalItems(100));
    }

    @Test
    void totalAcimaDe100EhAlta() {
        assertEquals(Severity.HIGH, Severity.fromTotalItems(101));
    }
}
