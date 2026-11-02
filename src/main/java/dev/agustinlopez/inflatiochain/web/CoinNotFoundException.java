package dev.agustinlopez.inflatiochain.web;

public class CoinNotFoundException extends RuntimeException {

	public CoinNotFoundException(String coinId) {
		super("Coin '" + coinId + "' not found");
	}
}
