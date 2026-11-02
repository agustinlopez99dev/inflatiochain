package dev.agustinlopez.inflatiochain.web;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import dev.agustinlopez.inflatiochain.config.RateLimitProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Per-IP token bucket on /api/**. Idle buckets are evicted so memory stays bounded. */
@Component
@ConditionalOnProperty(prefix = "inflatiochain.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitProperties props;
	private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.maximumSize(100_000)
			.build();

	public RateLimitFilter(RateLimitProperties props) {
		this.props = props;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Bucket bucket = buckets.get(clientIp(request), ip -> newBucket());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			chain.doFilter(request, response);
			return;
		}
		long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
		response.setStatus(429);
		response.setHeader("Retry-After", String.valueOf(retryAfter));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("""
				{"status":429,"title":"Too Many Requests","detail":"Rate limit exceeded, retry in %d seconds"}""".formatted(retryAfter));
	}

	private Bucket newBucket() {
		return Bucket.builder()
				.addLimit(limit -> limit.capacity(props.requestsPerMinute())
						.refillIntervally(props.requestsPerMinute(), Duration.ofMinutes(1)))
				.build();
	}

	private String clientIp(HttpServletRequest request) {
		if (props.trustForwardedFor()) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return request.getRemoteAddr();
	}
}
