package dev.agustinlopez.inflatiochain.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Subset of the CoinGecko payloads this app reads. */
public final class CoinGeckoDtos {

	private CoinGeckoDtos() {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Coin(
			String id,
			String name,
			String symbol,
			@JsonProperty("market_cap_rank") Integer marketCapRank,
			Image image,
			@JsonProperty("market_data") MarketData marketData) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Image(String thumb, String small, String large) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MarketData(
			@JsonProperty("current_price") Map<String, Double> currentPrice,
			@JsonProperty("market_cap") Map<String, Double> marketCap,
			@JsonProperty("circulating_supply") Double circulatingSupply,
			@JsonProperty("max_supply") Double maxSupply,
			@JsonProperty("max_supply_infinite") Boolean maxSupplyInfinite,
			Map<String, Double> ath,
			@JsonProperty("ath_change_percentage") Map<String, Double> athChangePercentage,
			@JsonProperty("ath_date") Map<String, String> athDate,
			@JsonProperty("price_change_percentage_24h") Double change24h,
			@JsonProperty("price_change_percentage_7d") Double change7d,
			@JsonProperty("price_change_percentage_14d") Double change14d,
			@JsonProperty("price_change_percentage_30d") Double change30d,
			@JsonProperty("price_change_percentage_60d") Double change60d,
			@JsonProperty("price_change_percentage_1y") Double change1y) {
	}

	/** Each series is a list of [timestampMillis, value] pairs. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MarketChart(
			List<List<Double>> prices,
			@JsonProperty("market_caps") List<List<Double>> marketCaps) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MarketListItem(
			String id,
			String name,
			String symbol,
			String image,
			@JsonProperty("market_cap_rank") Integer marketCapRank) {
	}
}
