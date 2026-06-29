package com.hologrammenu.head;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hologrammenu.HologramMenuMod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MinecraftHeadsCatalog {
	private static final String API_ROOT = "https://minecraft-heads.com/api/heads";
	private static final Gson GSON = new Gson();

	private enum LoadState {
		IDLE,
		LOADING,
		READY,
		FAILED,
		UNCONFIGURED
	}

	private static volatile LoadState state = LoadState.IDLE;
	private static List<HeadPresetEntry> allEntries = List.of();
	private static Map<String, HeadPresetEntry> byId = Map.of();
	private static Map<String, List<HeadPresetEntry>> byCategory = Map.of();
	private static String[] categories = HeadPresetCategories.PUBLIC.clone();

	private MinecraftHeadsCatalog() {
	}

	public static boolean isReady() {
		return state == LoadState.READY;
	}

	public static boolean isLoading() {
		return state == LoadState.LOADING;
	}

	public static boolean hasFailed() {
		return state == LoadState.FAILED || state == LoadState.UNCONFIGURED;
	}

	public static void startLoading() {
		if (state == LoadState.LOADING || state == LoadState.READY) {
			return;
		}
		MinecraftHeadsConfig config = MinecraftHeadsConfig.load();
		if (!config.hasAppUuid()) {
			state = LoadState.UNCONFIGURED;
			return;
		}
		state = LoadState.LOADING;
		Thread thread = new Thread(() -> load(config), "hologrammenu-minecraft-heads-catalog");
		thread.setDaemon(true);
		thread.start();
	}

	public static List<HeadPresetEntry> list(String category, String query, int page, int pageSize) {
		if (!isReady()) {
			return List.of();
		}
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<HeadPresetEntry> source;
		if (normalizedQuery.isEmpty()) {
			source = byCategory.getOrDefault(HeadPresetCategories.normalize(category), List.of());
		} else {
			source = new ArrayList<>();
			for (HeadPresetEntry entry : allEntries) {
				if (matchesQuery(entry, normalizedQuery)) {
					source.add(entry);
				}
			}
			source.sort(Comparator.comparing(HeadPresetEntry::name, String.CASE_INSENSITIVE_ORDER));
		}
		int start = Math.max(0, page) * pageSize;
		if (start >= source.size()) {
			return List.of();
		}
		return List.copyOf(source.subList(start, Math.min(source.size(), start + pageSize)));
	}

	public static int count(String category, String query) {
		if (!isReady()) {
			return 0;
		}
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (normalizedQuery.isEmpty()) {
			return byCategory.getOrDefault(HeadPresetCategories.normalize(category), List.of()).size();
		}
		int total = 0;
		for (HeadPresetEntry entry : allEntries) {
			if (matchesQuery(entry, normalizedQuery)) {
				total++;
			}
		}
		return total;
	}

	public static Optional<HeadPresetEntry> getEntry(String id) {
		if (!isReady() || id == null || id.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(byId.get(id.trim()));
	}

	public static String[] browseCategories() {
		return categories.clone();
	}

	private static void load(MinecraftHeadsConfig config) {
		try {
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
			Map<String, String> categoryNames = loadCategories(client, config);
			List<HeadPresetEntry> entries = loadHeads(client, config, categoryNames);
			install(entries, categoryNames);
			state = LoadState.READY;
			HologramMenuMod.LOGGER.info("Loaded {} Minecraft-Heads presets", allEntries.size());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			HologramMenuMod.LOGGER.warn("Failed to load Minecraft-Heads API catalog", exception);
			state = LoadState.FAILED;
		}
	}

	private static Map<String, String> loadCategories(HttpClient client, MinecraftHeadsConfig config) throws IOException, InterruptedException {
		JsonObject root = request(client, config, API_ROOT + "/categories?" + apiQuery(config, false));
		Map<String, String> result = new HashMap<>();
		JsonArray data = root.has("data") && root.get("data").isJsonArray() ? root.getAsJsonArray("data") : new JsonArray();
		for (JsonElement element : data) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject object = element.getAsJsonObject();
			String id = readString(object, "id");
			String name = readString(object, "n");
			if (!id.isBlank() && !name.isBlank()) {
				result.put(id, HeadPresetCategories.fromJsonCategory(name));
			}
		}
		return result;
	}

	private static List<HeadPresetEntry> loadHeads(
		HttpClient client,
		MinecraftHeadsConfig config,
		Map<String, String> categoryNames
	) throws IOException, InterruptedException {
		JsonObject root = request(client, config, API_ROOT + "/custom-heads?" + apiQuery(config, true) + "&id=true");
		List<HeadPresetEntry> entries = new ArrayList<>();
		JsonArray data = root.has("data") && root.get("data").isJsonArray() ? root.getAsJsonArray("data") : new JsonArray();
		collectHeads(data, entries, categoryNames);
		return entries;
	}

	private static void collectHeads(JsonArray data, List<HeadPresetEntry> entries, Map<String, String> categoryNames) {
		for (JsonElement element : data) {
			if (element.isJsonArray()) {
				collectHeads(element.getAsJsonArray(), entries, categoryNames);
				continue;
			}
			if (!element.isJsonObject()) {
				continue;
			}
			HeadPresetEntry entry = toEntry(element.getAsJsonObject(), categoryNames);
			if (entry != null) {
				entries.add(entry);
			}
		}
	}

	private static HeadPresetEntry toEntry(JsonObject object, Map<String, String> categoryNames) {
		String name = readString(object, "n");
		String textureHash = readString(object, "u");
		String value = readString(object, "v");
		if (textureHash.isBlank() && value.isBlank()) {
			return null;
		}
		String base64 = value.isBlank() ? HeadTextureEncoding.toBase64(textureHash) : value;
		if (base64.isBlank()) {
			return null;
		}
		String id = readString(object, "id");
		if (id.isBlank()) {
			id = textureHash.isBlank() ? Integer.toHexString(base64.hashCode()) : textureHash;
		}
		if (name.isBlank()) {
			name = id;
		}
		String categoryId = readString(object, "c");
		String category = categoryNames.getOrDefault(categoryId, "MISCELLANEOUS");
		return new HeadPresetEntry("mh:" + id, name, category, base64);
	}

	private static void install(List<HeadPresetEntry> entries, Map<String, String> categoryNames) {
		Map<String, HeadPresetEntry> idMap = new HashMap<>();
		Map<String, List<HeadPresetEntry>> categoryMap = new HashMap<>();
		List<HeadPresetEntry> sorted = new ArrayList<>(entries);
		sorted.sort(Comparator.comparing(HeadPresetEntry::name, String.CASE_INSENSITIVE_ORDER));
		for (HeadPresetEntry entry : sorted) {
			idMap.put(entry.id(), entry);
			categoryMap.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(entry);
		}
		for (List<HeadPresetEntry> categoryEntries : categoryMap.values()) {
			categoryEntries.sort(Comparator.comparing(HeadPresetEntry::name, String.CASE_INSENSITIVE_ORDER));
		}
		byId = Map.copyOf(idMap);
		byCategory = Map.copyOf(categoryMap);
		allEntries = List.copyOf(sorted);
		categories = categoryMap.keySet().stream()
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.toArray(String[]::new);
		if (categories.length == 0) {
			categories = categoryNames.values().stream()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toArray(String[]::new);
		}
		if (categories.length == 0) {
			categories = HeadPresetCategories.PUBLIC.clone();
		}
	}

	private static JsonObject request(HttpClient client, MinecraftHeadsConfig config, String url) throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(30))
			.header("Accept", "application/json")
			.header("User-Agent", "hologrammenu-mod/1.0")
			.GET();
		if (config.apiKey != null && !config.apiKey.isBlank()) {
			builder.header("api-key", config.apiKey.trim());
		}
		HttpResponse<InputStream> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
		byte[] body;
		try (InputStream input = response.body()) {
			body = input.readAllBytes();
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Minecraft-Heads API returned " + response.statusCode() + ": " + new String(body, StandardCharsets.UTF_8));
		}
		JsonElement parsed = GSON.fromJson(new String(body, StandardCharsets.UTF_8), JsonElement.class);
		if (parsed == null || !parsed.isJsonObject()) {
			throw new IOException("Minecraft-Heads API returned an invalid response");
		}
		return parsed.getAsJsonObject();
	}

	private static String apiQuery(MinecraftHeadsConfig config, boolean includeDemo) {
		StringBuilder builder = new StringBuilder("app_uuid=");
		builder.append(urlEncode(config.appUuid.trim()));
		if (includeDemo && config.demo) {
			builder.append("&demo=true");
		}
		return builder.toString();
	}

	private static String readString(JsonObject object, String key) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return "";
		}
		return element.getAsString().trim();
	}

	private static boolean matchesQuery(HeadPresetEntry entry, String query) {
		return entry.name().toLowerCase(Locale.ROOT).contains(query)
			|| entry.id().toLowerCase(Locale.ROOT).contains(query);
	}

	private static String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
