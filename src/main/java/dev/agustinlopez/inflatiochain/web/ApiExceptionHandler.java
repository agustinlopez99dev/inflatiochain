package dev.agustinlopez.inflatiochain.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(CoinNotFoundException.class)
	ProblemDetail notFound(CoinNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	/** CoinGecko rate limit: temporary, so 503 + Retry-After. Any other upstream failure is a 502. */
	@ExceptionHandler(UpstreamException.class)
	ResponseEntity<ProblemDetail> upstream(UpstreamException e) {
		if (e.isRateLimited()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.header(HttpHeaders.RETRY_AFTER, "60")
					.body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()));
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage()));
	}
}
