package com.paymentsapp.platform.error;

import java.util.Locale;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

// Ошибки в одном формате во всех сервисах: бизнес-отказы (DomainExceptionHandler) и ошибки формата запроса
// (InputErrorHandler). Язык ответов — русский для любого клиента: тексты Bean Validation («не должно быть пустым»)
// берутся по локали запроса, а заголовок Accept-Language у сервисов-клиентов может быть любым.
@AutoConfiguration(beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
@Import({DomainExceptionHandler.class, InputErrorHandler.class})
public class ErrorAutoConfiguration {

    private static final Locale RUSSIAN = Locale.of("ru");

    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(RUSSIAN);
    }
}
