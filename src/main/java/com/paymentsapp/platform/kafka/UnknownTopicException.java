package com.paymentsapp.platform.kafka;

import com.paymentsapp.platform.error.DomainException;
import com.paymentsapp.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;

// Топик не из списка platform.kafka.dead-letter.topics
class UnknownTopicException extends DomainException {

    UnknownTopicException(String topic) {
        super(Error.DLT_TOPIC_UNKNOWN, topic);
    }

    enum Error implements ErrorCode {
        DLT_TOPIC_UNKNOWN;

        @Override
        public HttpStatus status() {
            return HttpStatus.NOT_FOUND;
        }

        @Override
        public String template() {
            return "Сервис не читает топик %s";
        }
    }
}
