package dev.agustinlopez.inflatiochain.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.agustinlopez.inflatiochain.config.PrewarmProperties;
import dev.agustinlopez.inflatiochain.model.CoinListItem;
import dev.agustinlopez.inflatiochain.web.UpstreamException;

class PrewarmJobTest {

	static final class MutableClock extends Clock {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");

		@Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(java.time.ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}

	CoinService service = mock(CoinService.class);
	MutableClock clock = new MutableClock();
	AtomicInteger sleeps = new AtomicInteger();
	PrewarmProperties props = new PrewarmProperties(true, 2, Duration.ofMinutes(5), Duration.ZERO,
			Duration.ofSeconds(15), Duration.ofMinutes(2));
	PrewarmJob job = new PrewarmJob(service, props, clock, d -> sleeps.incrementAndGet());

	@BeforeEach
	void coins() {
		when(service.getCoinList()).thenReturn(List.of(
				new CoinListItem("a", "A", "a", null, 1),
				new CoinListItem("b", "B", "b", null, 2),
				new CoinListItem("c", "C", "c", null, 3)));
	}

	@Test
	void refreshesOnlyTopNAndThrottlesBetweenCoins() {
		job.run();

		verify(service).refreshSummary("a");
		verify(service).refreshSummary("b");
		verify(service, never()).refreshSummary("c");
		org.assertj.core.api.Assertions.assertThat(sleeps.get()).isEqualTo(2);
	}

	@Test
	void skipsCoinsThatAreStillFresh() {
		when(service.isSummaryFresh("a")).thenReturn(true);

		job.run();

		verify(service, never()).refreshSummary("a");
		verify(service).refreshSummary("b");
	}

	@Test
	void pausesAfterRateLimitUntilCooldownPasses() {
		org.mockito.Mockito.doThrow(new UpstreamException("limit", null, true)).when(service).refreshSummary("a");

		job.run();
		job.run(); // still cooling down
		verify(service, times(1)).refreshSummary(anyString());

		clock.now = clock.now.plus(Duration.ofMinutes(3));
		job.run();
		verify(service, times(2)).refreshSummary("a");
	}
}
