package dev.agustinlopez.inflatiochain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coingecko")
public record CoinGeckoProperties(
		String baseUrl,
		String userAgent,
		String apiKey,
		int historyDays,
		int listPages) {
}
