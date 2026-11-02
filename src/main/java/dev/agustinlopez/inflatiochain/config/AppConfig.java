package dev.agustinlopez.inflatiochain.config;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({CoinGeckoProperties.class, CacheProperties.class, PrewarmProperties.class, RateLimitProperties.class})
public class AppConfig {

	public static final String COIN_SUMMARY_CACHE = "coinSummary";
	public static final String COIN_LIST_CACHE = "coinList";
	/** Long-lived "last known good" copies, served only when CoinGecko fails. */
	public static final String COIN_SUMMARY_STALE_CACHE = "coinSummaryStale";
	public static final String COIN_LIST_STALE_CACHE = "coinListStale";

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
		CaffeineCache summaryStale = new CaffeineCache(COIN_SUMMARY_STALE_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(props.staleTtl())
				.maximumSize(props.summaryMaxSize())
				.recordStats()
				.build());
		CaffeineCache coinListStale = new CaffeineCache(COIN_LIST_STALE_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(props.staleTtl())
				.maximumSize(1)
				.recordStats()
				.build());
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(List.of(summary, coinList, summaryStale, coinListStale));
		return manager;
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
