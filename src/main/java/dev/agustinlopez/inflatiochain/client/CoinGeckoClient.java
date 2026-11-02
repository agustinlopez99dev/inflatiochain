package dev.agustinlopez.inflatiochain.client;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.Coin;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketChart;
import dev.agustinlopez.inflatiochain.client.CoinGeckoDtos.MarketListItem;
import dev.agustinlopez.inflatiochain.web.CoinNotFoundException;
import dev.agustinlopez.inflatiochain.web.UpstreamException;

@Component
public class CoinGeckoClient {

	private final RestClient restClient;

	public CoinGeckoClient(RestClient coinGeckoRestClient) {
		this.restClient = coinGeckoRestClient;
	}

	public Coin getCoin(String coinId) {
		return call(coinId, () -> restClient.get()
				.uri("/coins/{id}", coinId)
				.retrieve()
				.body(Coin.class));
	}

	public MarketChart getMarketChart(String coinId, int days) {
		return call(coinId, () -> restClient.get()
				.uri("/coins/{id}/market_chart?vs_currency=usd&days={days}", coinId, days)
				.retrieve()
				.body(MarketChart.class));
	}

	public List<MarketListItem> getMarkets(int page, int perPage) {
		return call(null, () -> restClient.get()
				.uri("/coins/markets?vs_currency=usd&order=market_cap_desc&per_page={perPage}&page={page}", perPage, page)
				.retrieve()
				.body(new ParameterizedTypeReference<List<MarketListItem>>() { }));
	}

	private <T> T call(String coinId, Supplier<T> request) {
		try {
			return request.get();
		}
		catch (RestClientResponseException e) {
			if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) && coinId != null) {
				throw new CoinNotFoundException(coinId);
			}
			if (e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
				throw new UpstreamException("CoinGecko rate limit reached, try again in a minute", e);
			}
			throw new UpstreamException("CoinGecko responded with status " + e.getStatusCode().value(), e);
		}
		catch (RestClientException e) {
			throw new UpstreamException("Could not reach CoinGecko", e);
		}
	}
}
