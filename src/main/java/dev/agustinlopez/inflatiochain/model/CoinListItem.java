package dev.agustinlopez.inflatiochain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoinListItem(
		String id,
		String name,
		String symbol,
		String image,
		@JsonProperty("market_cap_rank") Integer marketCapRank) {
}
