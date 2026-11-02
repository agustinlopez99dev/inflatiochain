package dev.agustinlopez.inflatiochain.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inflatiochain.cache")
public record CacheProperties(
		Duration summaryTtl,
		long summaryMaxSize,
		Duration coinListTtl,
		Duration staleTtl) {
}
