package dev.agustinlopez.inflatiochain.service;

import java.time.Duration;

/** Seam over Thread.sleep so throttling can be tested without waiting. */
@FunctionalInterface
public interface Sleeper {

	void sleep(Duration duration) throws InterruptedException;
}
