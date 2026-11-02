package dev.agustinlopez.inflatiochain.web;

import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.agustinlopez.inflatiochain.model.CoinListItem;
import dev.agustinlopez.inflatiochain.model.CoinSummary;
import dev.agustinlopez.inflatiochain.service.CoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coins")
public class CoinController {

	private final CoinService service;

	public CoinController(CoinService service) {
		this.service = service;
	}

	@GetMapping("/coins/list")
	@Operation(summary = "Top coins by market cap (id, name, symbol, image, rank). Cached 1 day.")
	public List<CoinListItem> coinList() {
		return service.getCoinList();
	}

	@GetMapping("/coin/{coinId}")
	@Operation(summary = "Market data plus supply inflation over the last year for a CoinGecko coin id. Cached 5 minutes.")
	public CoinSummary coin(@PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,100}") String coinId) {
		return service.getCoinSummary(coinId.toLowerCase(Locale.ROOT));
	}
}
