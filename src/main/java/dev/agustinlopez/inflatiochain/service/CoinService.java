package dev.agustinlopez.inflatiochain.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import dev.agustinlopez.inflatiochain.client.CoinGeckoClient;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.Coin;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketData;
import dev.agustinlopez.inflatiochain.config.AppConfig;
import dev.agustinlopez.inflatiochain.config.CoinGeckoProperties;
import dev.agustinlopez.inflatiochain.model.CoinListItem;
import dev.agustinlopez.inflatiochain.model.CoinSummary;
import dev.agustinlopez.inflatiochain.service.InflationCalculator.OldestData;

@Service
public class CoinService {

	private static final int COINS_PER_PAGE = 250;
	private static final String USD = "usd";

	private final CoinGeckoClient client;
	private final CoinGeckoProperties props;

	public CoinService(CoinGeckoClient client, CoinGeckoProperties props) {
		this.client = client;
		this.props = props;
	}

	@Cacheable(AppConfig.COIN_LIST_CACHE)
	public List<CoinListItem> getCoinList() {
		return IntStream.rangeClosed(1, props.listPages())
				.boxed()
				.flatMap(page -> client.getMarkets(page, COINS_PER_PAGE).stream())
				.map(c -> new CoinListItem(c.id(), c.name(), c.symbol(), c.image(), c.marketCapRank()))
				.toList();
	}

	@Cacheable(cacheNames = AppConfig.COIN_SUMMARY_CACHE, key = "#coinId")
	public CoinSummary getCoinSummary(String coinId) {
		Coin coin = client.getCoin(coinId);
		Instant now = Instant.now();
		OldestData old = InflationCalculator.oldestData(client.getMarketChart(coinId, props.historyDays()), now);

		MarketData md = coin.marketData();
		Double circulating = md.circulatingSupply();
		Double emission = InflationCalculator.emissionPercentage(circulating, old.circulatingSupply());
		String note = old.daysSince() == null
				? "No historical data available"
				: "Oldest data is from " + old.daysSince() + " days ago";

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
				now.getEpochSecond());
	}

	private static Double usd(Map<String, Double> byCurrency) {
		return byCurrency == null ? null : byCurrency.get(USD);
	}
}
