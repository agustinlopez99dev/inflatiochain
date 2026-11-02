package dev.agustinlopez.inflatiochain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import dev.agustinlopez.inflatiochain.client.CoinGeckoClient;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.Coin;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketData;
import dev.agustinlopez.inflatiochain.config.AppConfig;
import dev.agustinlopez.inflatiochain.config.CoinGeckoProperties;
import dev.agustinlopez.inflatiochain.model.CoinListItem;
import dev.agustinlopez.inflatiochain.model.CoinSummary;
import dev.agustinlopez.inflatiochain.service.InflationCalculator.OldestData;
import dev.agustinlopez.inflatiochain.web.UpstreamException;

@Service
public class CoinService {

	private static final Logger log = LoggerFactory.getLogger(CoinService.class);
	private static final int COINS_PER_PAGE = 250;
	private static final String USD = "usd";
	private static final String LIST_KEY = "all";

	private final CoinGeckoClient client;
	private final CoinGeckoProperties props;
	private final Cache summaryCache;
	private final Cache summaryStaleCache;
	private final Cache listCache;
	private final Cache listStaleCache;

	public CoinService(CoinGeckoClient client, CoinGeckoProperties props, CacheManager caches) {
		this.client = client;
		this.props = props;
		this.summaryCache = caches.getCache(AppConfig.COIN_SUMMARY_CACHE);
		this.summaryStaleCache = caches.getCache(AppConfig.COIN_SUMMARY_STALE_CACHE);
		this.listCache = caches.getCache(AppConfig.COIN_LIST_CACHE);
		this.listStaleCache = caches.getCache(AppConfig.COIN_LIST_STALE_CACHE);
	}

	public List<CoinListItem> getCoinList() {
		return cached(listCache, listStaleCache, LIST_KEY, this::fetchCoinList, UnaryOperator.identity());
	}

	public CoinSummary getCoinSummary(String coinId) {
		return cached(summaryCache, summaryStaleCache, coinId, () -> buildSummary(coinId), CoinSummary::asStale);
	}

	public boolean isSummaryFresh(String coinId) {
		return summaryCache.get(coinId) != null;
	}

	/** Rebuilds a summary regardless of what is cached and stores it in both caches. */
	public void refreshSummary(String coinId) {
		CoinSummary summary = buildSummary(coinId);
		summaryCache.put(coinId, summary);
		summaryStaleCache.put(coinId, summary);
	}

	/**
	 * Concurrent misses for the same key share one load (Cache#get with a loader is atomic per key).
	 * If CoinGecko fails and a last-known-good copy exists it is returned; 404s and other errors are not masked.
	 */
	@SuppressWarnings("unchecked")
	private <T> T cached(Cache fresh, Cache stale, String key, Supplier<T> loader, UnaryOperator<T> markStale) {
		try {
			return fresh.get(key, () -> {
				T value = loader.get();
				stale.put(key, value);
				return value;
			});
		}
		catch (RuntimeException e) {
			Throwable cause = (e instanceof Cache.ValueRetrievalException && e.getCause() != null) ? e.getCause() : e;
			if (cause instanceof UpstreamException) {
				Cache.ValueWrapper last = stale.get(key);
				if (last != null) {
					log.warn("CoinGecko failed for '{}' ({}); serving stale copy", key, cause.getMessage());
					return markStale.apply((T) last.get());
				}
			}
			if (cause instanceof RuntimeException re) {
				throw re;
			}
			throw e;
		}
	}

	private List<CoinListItem> fetchCoinList() {
		return IntStream.rangeClosed(1, props.listPages())
				.boxed()
				.flatMap(page -> client.getMarkets(page, COINS_PER_PAGE).stream())
				.map(c -> new CoinListItem(c.id(), c.name(), c.symbol(), c.image(), c.marketCapRank()))
				.toList();
	}

	private CoinSummary buildSummary(String coinId) {
		Coin coin = client.getCoin(coinId);
		Instant now = Instant.now();
		OldestData old = InflationCalculator.oldestData(client.getMarketChart(coinId, props.historyDays()), now);

		MarketData md = coin.marketData();
		Double circulating = md.circulatingSupply();
		Double emission = InflationCalculator.emissionPercentage(circulating, old.circulatingSupply());
		String note = old.daysSince() == null
				? "No historical data available"
				: "Oldest data is from " + old.daysSince() + " days ago; old supply estimated as market_cap / price";

		return new CoinSummary(
				coin.id(), coin.name(), coin.symbol(),
				usd(md.currentPrice()), usd(md.marketCap()), coin.marketCapRank(),
				circulating, md.maxSupply(), md.maxSupplyInfinite(),
				usd(md.ath()), usd(md.athChangePercentage()), md.athDate() == null ? null : md.athDate().get(USD),
				md.change24h(), md.change7d(), md.change14d(), md.change30d(), md.change60d(), md.change1y(),
				coin.image() == null ? null : coin.image().thumb(),
				coin.image() == null ? null : coin.image().small(),
				coin.image() == null ? null : coin.image().large(),
				old.price(), old.marketCap(), old.circulatingSupply(), old.daysSince(), emission, note,
				old.circulatingSupply() != null, old.daysSince(), false,
				now.getEpochSecond());
	}

	private static Double usd(Map<String, Double> byCurrency) {
		return byCurrency == null ? null : byCurrency.get(USD);
	}
}
