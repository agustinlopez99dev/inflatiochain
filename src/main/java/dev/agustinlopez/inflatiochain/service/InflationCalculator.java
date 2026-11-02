package dev.agustinlopez.inflatiochain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketChart;

/** Pure logic: derives the oldest known supply from a market chart and the emission since then. */
public final class InflationCalculator {

	private InflationCalculator() {
	}

	/** Oldest datapoint of a market chart; every field is null when there is no usable history. */
	public record OldestData(Double price, Double marketCap, Double circulatingSupply, Long daysSince) {

		static final OldestData NONE = new OldestData(null, null, null, null);
	}

	public static OldestData oldestData(MarketChart chart, Instant now) {
		if (chart == null || chart.prices() == null || chart.prices().isEmpty()) {
			return OldestData.NONE;
		}
		List<Double> firstPrice = chart.prices().get(0);
		Instant timestamp = Instant.ofEpochMilli(firstPrice.get(0).longValue());
		Double price = firstPrice.get(1);
		long daysSince = ChronoUnit.DAYS.between(timestamp, now);

		Double marketCap = null;
		if (chart.marketCaps() != null && !chart.marketCaps().isEmpty()) {
			marketCap = chart.marketCaps().get(0).get(1);
		}
		Double supply = (price != null && price > 0 && marketCap != null) ? marketCap / price : null;
		return new OldestData(price, marketCap, supply, daysSince);
	}

	/** Percentage growth of the circulating supply, or null when it cannot be computed. */
	public static Double emissionPercentage(Double currentSupply, Double oldSupply) {
		if (currentSupply == null || oldSupply == null || oldSupply <= 0) {
			return null;
		}
		return (currentSupply - oldSupply) / oldSupply * 100;
	}
}
