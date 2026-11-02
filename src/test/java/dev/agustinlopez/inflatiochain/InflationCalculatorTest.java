package dev.agustinlopez.inflatiochain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketChart;
import dev.agustinlopez.inflatiochain.service.InflationCalculator;
import dev.agustinlopez.inflatiochain.service.InflationCalculator.OldestData;

class InflationCalculatorTest {

	private static final Instant NOW = Instant.parse("2026-01-31T00:00:00Z");

	@Test
	void derivesOldSupplyFromMarketCapOverPrice() {
		long tenDaysAgo = NOW.minusSeconds(10 * 86_400L + 60).toEpochMilli();
		MarketChart chart = new MarketChart(
				List.of(List.of((double) tenDaysAgo, 50.0)),
				List.of(List.of((double) tenDaysAgo, 5_000.0)));

		OldestData old = InflationCalculator.oldestData(chart, NOW);

		assertThat(old.price()).isEqualTo(50.0);
		assertThat(old.circulatingSupply()).isEqualTo(100.0);
		assertThat(old.daysSince()).isEqualTo(10);
	}

	@Test
	void emptyHistoryYieldsNulls() {
		OldestData old = InflationCalculator.oldestData(new MarketChart(List.of(), List.of()), NOW);

		assertThat(old.price()).isNull();
		assertThat(old.circulatingSupply()).isNull();
		assertThat(old.daysSince()).isNull();
	}

	@Test
	void missingMarketCapLeavesSupplyNull() {
		MarketChart chart = new MarketChart(List.of(List.of(0.0, 10.0)), List.of());

		assertThat(InflationCalculator.oldestData(chart, NOW).circulatingSupply()).isNull();
	}

	@Test
	void emissionIsPercentageGrowth() {
		assertThat(InflationCalculator.emissionPercentage(110.0, 100.0)).isEqualTo(10.0);
		assertThat(InflationCalculator.emissionPercentage(90.0, 100.0)).isEqualTo(-10.0);
	}

	@Test
	void emissionIsNullWhenNotComputable() {
		assertThat(InflationCalculator.emissionPercentage(null, 100.0)).isNull();
		assertThat(InflationCalculator.emissionPercentage(100.0, null)).isNull();
		assertThat(InflationCalculator.emissionPercentage(100.0, 0.0)).isNull();
	}
}
