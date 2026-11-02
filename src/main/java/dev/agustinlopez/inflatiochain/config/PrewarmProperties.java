package dev.agustinlopez.inflatiochain.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inflatiochain.prewarm")
public record PrewarmProperties(
		boolean enabled,
		int topN,
		Duration interval,
		Duration initialDelay,
		Duration throttle,
		Duration cooldown) {
}
