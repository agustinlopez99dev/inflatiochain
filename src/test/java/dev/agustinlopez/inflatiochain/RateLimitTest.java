package dev.agustinlopez.inflatiochain;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "inflatiochain.rate-limit.requests-per-minute=3")
@AutoConfigureMockMvc
class RateLimitTest {

	@Autowired MockMvc mvc;

	static RequestPostProcessor fromIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

	// An invalid id answers 400 without touching CoinGecko, so only the filter is exercised.
	private static final String LOCAL_ONLY = "/api/v1/coin/bad%20id!";

	@Test
	void blocksTheRequestOverTheLimitWithRetryAfter() throws Exception {
		for (int i = 0; i < 3; i++) {
			mvc.perform(get(LOCAL_ONLY).with(fromIp("10.0.0.1"))).andExpect(status().isBadRequest());
		}

		mvc.perform(get(LOCAL_ONLY).with(fromIp("10.0.0.1")))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

	@Test
	void otherClientsAreUnaffected() throws Exception {
		for (int i = 0; i < 4; i++) {
			mvc.perform(get(LOCAL_ONLY).with(fromIp("10.0.0.2")));
		}

		mvc.perform(get(LOCAL_ONLY).with(fromIp("10.0.0.3"))).andExpect(status().isBadRequest());
	}

	@Test
	void nonApiPathsAreNotLimited() throws Exception {
		for (int i = 0; i < 6; i++) {
			mvc.perform(get("/actuator/health").with(fromIp("10.0.0.4"))).andExpect(status().isOk());
		}
	}
}
