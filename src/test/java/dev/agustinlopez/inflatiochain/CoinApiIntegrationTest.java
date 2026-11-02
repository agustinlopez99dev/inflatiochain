package dev.agustinlopez.inflatiochain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest
@AutoConfigureMockMvc
class CoinApiIntegrationTest {

	static final MockWebServer COINGECKO = new MockWebServer();

	static {
		try {
			COINGECKO.setDispatcher(new Dispatcher() {
				@Override
				public MockResponse dispatch(RecordedRequest request) {
					String path = request.getPath();
					if (path.startsWith("/coins/markets")) {
						return json(200, """
								[{"id":"bitcoin","name":"Bitcoin","symbol":"btc","image":"b.png","market_cap_rank":1}]""");
					}
					if (path.startsWith("/coins/bitcoin/market_chart")) {
						long yearAgo = Instant.now().minusSeconds(365 * 86_400L).toEpochMilli();
						return json(200, """
								{"prices":[[%d,50.0]],"market_caps":[[%d,5000.0]]}""".formatted(yearAgo, yearAgo));
					}
					if (path.startsWith("/coins/bitcoin")) {
						return json(200, """
								{"id":"bitcoin","name":"Bitcoin","symbol":"btc","market_cap_rank":1,
								 "image":{"thumb":"t","small":"s","large":"l"},
								 "market_data":{"current_price":{"usd":60000.5},"market_cap":{"usd":1.0e12},
								   "circulating_supply":110.0,"max_supply":21000000,"max_supply_infinite":false,
								   "ath":{"usd":70000},"ath_change_percentage":{"usd":-14.2},"ath_date":{"usd":"2021-11-10T14:24:11.849Z"},
								   "price_change_percentage_24h":1.5,"price_change_percentage_7d":2.5,
								   "price_change_percentage_14d":3.5,"price_change_percentage_30d":4.5,
								   "price_change_percentage_60d":5.5,"price_change_percentage_1y":6.5}}""");
					}
					return json(404, "{\"error\":\"coin not found\"}");
				}
			});
			COINGECKO.start();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static MockResponse json(int status, String body) {
		return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body);
	}

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("coingecko.base-url", () -> COINGECKO.url("/").toString().replaceAll("/$", ""));
	}

	@AfterAll
	static void stop() throws IOException {
		COINGECKO.shutdown();
	}

	@Autowired MockMvc mvc;
	@Autowired CacheManager caches;

	@BeforeEach
	void clearCaches() {
		caches.getCacheNames().forEach(n -> caches.getCache(n).clear());
	}

	@Test
	void coinSummaryComputesInflationAndKeepsSnakeCaseContract() throws Exception {
		mvc.perform(get("/api/v1/coin/Bitcoin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("bitcoin"))
				.andExpect(jsonPath("$.price").value(60000.5))
				.andExpect(jsonPath("$.market_cap_rank").value(1))
				.andExpect(jsonPath("$.price_change_percentage_24h").value(1.5))
				.andExpect(jsonPath("$.image_large").value("l"))
				.andExpect(jsonPath("$.old_circulating_supply").value(100.0))
				.andExpect(jsonPath("$.circulating_emission_percentage").value(10.0))
				.andExpect(jsonPath("$.last_updated").isNumber());
	}

	@Test
	void secondRequestIsServedFromCache() throws Exception {
		int before = COINGECKO.getRequestCount();

		mvc.perform(get("/api/v1/coin/bitcoin")).andExpect(status().isOk());
		int afterFirst = COINGECKO.getRequestCount();
		mvc.perform(get("/api/v1/coin/bitcoin")).andExpect(status().isOk());

		assertThat(afterFirst - before).isEqualTo(2);
		assertThat(COINGECKO.getRequestCount()).isEqualTo(afterFirst);
	}

	@Test
	void coinListIsReturned() throws Exception {
		mvc.perform(get("/api/v1/coins/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("bitcoin"))
				.andExpect(jsonPath("$[0].market_cap_rank").value(1));
	}

	@Test
	void unknownCoinIs404() throws Exception {
		mvc.perform(get("/api/v1/coin/doesnotexist")).andExpect(status().isNotFound());
	}

	@Test
	void malformedIdIs400() throws Exception {
		mvc.perform(get("/api/v1/coin/bad%20id!")).andExpect(status().isBadRequest());
	}
}
