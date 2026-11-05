package com.paymentsapp.platform.resilience;

import org.springframework.web.client.RestClientException;

// Сосед недоступен: circuit breaker разомкнут или исчерпан лимит одновременных вызовов.
// Наследник RestClientException — для вызывающего кода это тот же «сбой связи», что и тайм-аут.
public class ServiceUnavailableException extends RestClientException {

    private final String backend;

    public ServiceUnavailableException(String backend, Throwable cause) {
        super("Сервис " + backend + " недоступен: " + cause.getMessage(), cause);
        this.backend = backend;
    }

    public String getBackend() {
        return backend;
    }
}
