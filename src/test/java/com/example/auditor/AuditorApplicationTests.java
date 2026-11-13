package com.example.auditor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// O consumer da DLQ fica desligado: o teste só confere se o contexto Spring sobe.
@SpringBootTest(properties = "auditor.consumer.enabled=false")
class AuditorApplicationTests {

    @Test
    void contextLoads() {
    }
}
