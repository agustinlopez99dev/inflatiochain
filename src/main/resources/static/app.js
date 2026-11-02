const input = document.getElementById("coin");
const message = document.getElementById("message");
const card = document.getElementById("card");
const ids = new Set();

const $ = (id) => document.getElementById(id);
const num = (v, digits = 2) => v == null ? "N/A" : Number(v).toLocaleString(undefined, { maximumFractionDigits: digits });
const usd = (v) => v == null ? "N/A" : "$" + num(v, v >= 1 ? 2 : 6);

function show(text) {
	message.textContent = text;
	card.hidden = true;
}

async function load(id) {
	message.textContent = "Loading…";
	let res;
	try {
		res = await fetch(`/api/v1/coin/${encodeURIComponent(id)}`);
	} catch {
		return show("Could not reach the server.");
	}
	if (res.status === 404) return show(`Coin '${id}' not found.`);
	if (res.status === 429) return show(`Too many requests, retry in ${res.headers.get("Retry-After") || "a few"} seconds.`);
	if (res.status === 503) return show("CoinGecko's free rate limit was reached. Wait about a minute and try again.");
	if (res.status === 502) return show("CoinGecko is unavailable, try again shortly.");
	if (!res.ok) return show(`Unexpected error (HTTP ${res.status}).`);
	render(await res.json());
}

function render(c) {
	message.textContent = "";
	$("logo").src = c.image_small || "";
	$("name").textContent = `${c.name} (${c.symbol.toUpperCase()})`;
	$("price").textContent = usd(c.price);
	$("cap").textContent = "Market cap " + usd(c.market_cap);
	$("inflation").textContent = c.circulating_emission_percentage == null ? "N/A" : num(c.circulating_emission_percentage) + "%";
	$("supply").textContent = num(c.circulating_supply, 0);
	$("old-supply").textContent = num(c.old_circulating_supply, 0);
	$("window").textContent = c.window_days ?? "?";
	$("estimated").hidden = !c.supply_estimated;
	$("stale").hidden = !c.stale;
	$("updated").textContent = new Date(c.last_updated * 1000).toLocaleString();
	card.hidden = false;
}

input.addEventListener("change", () => {
	const id = input.value.trim().toLowerCase();
	if (!id) return;
	if (ids.size && !ids.has(id)) return show(`'${id}' is not in the coin list.`);
	load(id);
});

(async () => {
	try {
		const coins = await (await fetch("/api/v1/coins/list")).json();
		const list = $("coins");
		for (const c of coins) {
			ids.add(c.id);
			const option = document.createElement("option");
			option.value = c.id;
			option.label = `${c.name} (${c.symbol.toUpperCase()})`;
			list.appendChild(option);
		}
	} catch {
		// the list is a convenience; typing a valid id still works
	}
	input.value = "bitcoin";
	load("bitcoin");
})();
