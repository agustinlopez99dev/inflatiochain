package dev.agustinlopez.inflatiochain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inflatiochain.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		int requestsPerMinute,
		boolean trustForwardedFor) {
}
