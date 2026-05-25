package com.example.auditor.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws.sqs.queue")
public class DlqProperties {

    private String entradaPedido;
    private String region = "us-east-1";

    public String getEntradaPedido() {
        return entradaPedido;
    }

    public void setEntradaPedido(String entradaPedido) {
        this.entradaPedido = entradaPedido;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
