package dev.agustinlopez.inflatiochain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Same JSON contract as the original Flask API, with null for anything CoinGecko did not provide. */
public record CoinSummary(
		String id,
		String name,
		String symbol,

		Double price,
		@JsonProperty("market_cap") Double marketCap,
		@JsonProperty("market_cap_rank") Integer marketCapRank,

		@JsonProperty("circulating_supply") Double circulatingSupply,
		@JsonProperty("max_supply") Double maxSupply,
		@JsonProperty("max_supply_is_infinity") Boolean maxSupplyIsInfinity,

		Double ath,
		@JsonProperty("ath_change_percentage") Double athChangePercentage,
		@JsonProperty("ath_date") String athDate,

		@JsonProperty("price_change_percentage_24h") Double priceChangePercentage24h,
		@JsonProperty("price_change_percentage_7d") Double priceChangePercentage7d,
		@JsonProperty("price_change_percentage_14d") Double priceChangePercentage14d,
		@JsonProperty("price_change_percentage_30d") Double priceChangePercentage30d,
		@JsonProperty("price_change_percentage_60d") Double priceChangePercentage60d,
		@JsonProperty("price_change_percentage_1y") Double priceChangePercentage1y,

		@JsonProperty("image_thumb") String imageThumb,
		@JsonProperty("image_small") String imageSmall,
		@JsonProperty("image_large") String imageLarge,

		@JsonProperty("old_price") Double oldPrice,
		@JsonProperty("old_market_cap") Double oldMarketCap,
		@JsonProperty("old_circulating_supply") Double oldCirculatingSupply,
		@JsonProperty("days_since_old_data") Long daysSinceOldData,
		@JsonProperty("circulating_emission_percentage") Double circulatingEmissionPercentage,
		@JsonProperty("historical_note") String historicalNote,

		/** Epoch seconds when this summary was built (i.e. how old the cached data is). */
		@JsonProperty("last_updated") long lastUpdated) {
}
