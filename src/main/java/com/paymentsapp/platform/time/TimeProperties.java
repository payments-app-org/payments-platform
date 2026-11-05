package com.paymentsapp.platform.time;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Часовой пояс бизнеса: по нему считаются дневные и месячные лимиты, «сегодня» планировщиков, дни в истории
@ConfigurationProperties("platform.time")
public record TimeProperties(@DefaultValue("Europe/Moscow") ZoneId zone) {
}
