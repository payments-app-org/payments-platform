package com.paymentsapp.platform.resilience;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.function.Predicate;

// Предикат для retry: повторяем, только если запрос точно не дошёл до соседа (не удалось соединиться).
// Для неидемпотентных вызовов, где тайм-аут чтения означает «исход неизвестен» (перевод в процессинг).
// В application.yml: resilience4j.retry.instances.<имя>.retry-exception-predicate.
public class ConnectFailure implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof ConnectException || e instanceof HttpConnectTimeoutException
                    || e instanceof UnknownHostException || e instanceof UnresolvedAddressException) {
                return true;
            }
        }
        return false;
    }
}
