package dev.agustinlopez.inflatiochain.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.agustinlopez.inflatiochain.config.PrewarmProperties;
import dev.agustinlopez.inflatiochain.model.CoinListItem;
import dev.agustinlopez.inflatiochain.web.UpstreamException;

/**
 * Keeps the top coins warm so users rarely trigger a CoinGecko call. Coins are refreshed one at a time with a
 * pause between them (free tier: ~10 req/min, 2 calls per coin) and the job backs off after a 429.
 */
@Component
@ConditionalOnProperty(prefix = "inflatiochain.prewarm", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PrewarmJob {

	private static final Logger log = LoggerFactory.getLogger(PrewarmJob.class);

	private final CoinService service;
	private final PrewarmProperties props;
	private final Clock clock;
	private final Sleeper sleeper;
	private Instant coolDownUntil = Instant.MIN;

	@Autowired
	public PrewarmJob(CoinService service, PrewarmProperties props, Clock clock) {
		this(service, props, clock, d -> Thread.sleep(d));
	}

	PrewarmJob(CoinService service, PrewarmProperties props, Clock clock, Sleeper sleeper) {
		this.service = service;
		this.props = props;
		this.clock = clock;
		this.sleeper = sleeper;
	}

	@Scheduled(fixedDelayString = "${inflatiochain.prewarm.interval}", initialDelayString = "${inflatiochain.prewarm.initial-delay}")
	public void run() {
		if (clock.instant().isBefore(coolDownUntil)) {
			log.debug("Prewarm skipped, cooling down until {}", coolDownUntil);
			return;
		}
		try {
			List<CoinListItem> top = service.getCoinList().stream().limit(props.topN()).toList();
			for (CoinListItem coin : top) {
				if (service.isSummaryFresh(coin.id())) {
					continue;
				}
				service.refreshSummary(coin.id());
				sleeper.sleep(props.throttle());
			}
		}
		catch (UpstreamException e) {
			if (e.isRateLimited()) {
				coolDownUntil = clock.instant().plus(props.cooldown());
				log.warn("Prewarm hit CoinGecko rate limit, pausing until {}", coolDownUntil);
			}
			else {
				log.warn("Prewarm stopped: {}", e.getMessage());
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
