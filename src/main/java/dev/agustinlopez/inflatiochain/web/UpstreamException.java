package dev.agustinlopez.inflatiochain.web;

public class UpstreamException extends RuntimeException {

	private final boolean rateLimited;

	public UpstreamException(String message, Throwable cause) {
		this(message, cause, false);
	}

	public UpstreamException(String message, Throwable cause, boolean rateLimited) {
		super(message, cause);
		this.rateLimited = rateLimited;
	}

	/** True when CoinGecko answered 429. */
	public boolean isRateLimited() {
		return rateLimited;
	}
}
