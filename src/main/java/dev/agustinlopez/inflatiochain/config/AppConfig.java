package dev.agustinlopez.inflatiochain.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
@EnableConfigurationProperties({CoinGeckoProperties.class, CacheProperties.class})
public class AppConfig {

	public static final String COIN_SUMMARY_CACHE = "coinSummary";
	public static final String COIN_LIST_CACHE = "coinList";

	@Bean
	RestClient coinGeckoRestClient(CoinGeckoProperties props) {
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(props.baseUrl())
				.defaultHeader(HttpHeaders.USER_AGENT, props.userAgent());
		if (StringUtils.hasText(props.apiKey())) {
			builder.defaultHeader("x-cg-demo-api-key", props.apiKey());
		}
		return builder.build();
	}

	/** Each cache has its own TTL and size, which a single CaffeineCacheManager spec cannot express. */
	@Bean
	CacheManager cacheManager(CacheProperties props) {
		CaffeineCache summary = new CaffeineCache(COIN_SUMMARY_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(props.summaryTtl())
				.maximumSize(props.summaryMaxSize())
				.recordStats()
				.build());
		CaffeineCache coinList = new CaffeineCache(COIN_LIST_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(props.coinListTtl())
				.maximumSize(1)
				.recordStats()
				.build());
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(List.of(summary, coinList));
		return manager;
	}
}
