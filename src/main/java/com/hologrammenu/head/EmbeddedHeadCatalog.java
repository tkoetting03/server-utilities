package com.hologrammenu.head;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hologrammenu.HologramMenuMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EmbeddedHeadCatalog {
	private static final String HEADS_URL = "https://raw.githubusercontent.com/TheSilentPro/heads/main/heads.json";
	private static final Path CACHE_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(HologramMenuMod.MOD_ID)
		.resolve("heads.json");
	private static final Gson GSON = new Gson();
	private static final Type RAW_HEAD_LIST = new TypeToken<List<RawHead>>() {}.getType();

	private enum LoadState {
		IDLE,
		LOADING,
		READY,
		FAILED
	}

	private static volatile LoadState state = LoadState.IDLE;
	private static Map<String, List<HeadPresetEntry>> byCategory = Map.of();
	private static Map<String, HeadPresetEntry> byId = Map.of();
	private static List<HeadPresetEntry> allEntries = List.of();

	private EmbeddedHeadCatalog() {
	}

	public static boolean isReady() {
		return state == LoadState.READY;
	}

	public static boolean isLoading() {
		return state == LoadState.LOADING;
	}

	public static boolean hasFailed() {
		return state == LoadState.FAILED;
	}

	public static void startLoading() {
		if (state == LoadState.LOADING || state == LoadState.READY) {
			return;
		}
		state = LoadState.LOADING;
		Thread thread = new Thread(EmbeddedHeadCatalog::load, "hologrammenu-head-catalog");
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
			String normalizedCategory = HeadPresetCategories.normalize(category);
			source = byCategory.getOrDefault(normalizedCategory, List.of());
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
		int end = Math.min(source.size(), start + pageSize);
		return List.copyOf(source.subList(start, end));
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

	public static Optional<String> getBase64(String id) {
		return getEntry(id).map(HeadPresetEntry::base64);
	}

	private static void load() {
		try {
			byte[] data = readData();
			parse(data);
			state = LoadState.READY;
			HologramMenuMod.LOGGER.info("Loaded {} embedded Head Database presets", allEntries.size());
		} catch (IOException | InterruptedException exception) {
			if (exception instanceof InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
			}
			HologramMenuMod.LOGGER.error("Failed to load embedded Head Database catalog", exception);
			state = LoadState.FAILED;
		}
	}

	private static byte[] readData() throws IOException, InterruptedException {
		if (Files.exists(CACHE_PATH) && Files.size(CACHE_PATH) > 0L) {
			return Files.readAllBytes(CACHE_PATH);
		}
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(HEADS_URL))
			.timeout(Duration.ofMinutes(2))
			.GET()
			.build();
		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Head Database download failed with status " + response.statusCode());
		}
		byte[] data;
		try (InputStream input = response.body()) {
			data = input.readAllBytes();
		}
		if (data.length == 0) {
			throw new IOException("Head Database download returned no data");
		}
		Files.createDirectories(CACHE_PATH.getParent());
		Files.write(CACHE_PATH, data);
		return data;
	}

	private static void parse(byte[] data) {
		List<RawHead> rawHeads = GSON.fromJson(new String(data, java.nio.charset.StandardCharsets.UTF_8), RAW_HEAD_LIST);
		if (rawHeads == null || rawHeads.isEmpty()) {
			byCategory = Map.of();
			byId = Map.of();
			allEntries = List.of();
			return;
		}
		Map<String, List<HeadPresetEntry>> categoryMap = new HashMap<>();
		Map<String, HeadPresetEntry> idMap = new HashMap<>();
		List<HeadPresetEntry> entries = new ArrayList<>(rawHeads.size());
		for (RawHead rawHead : rawHeads) {
			HeadPresetEntry entry = toEntry(rawHead);
			if (entry == null) {
				continue;
			}
			entries.add(entry);
			idMap.put(entry.id(), entry);
			categoryMap.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(entry);
		}
		for (List<HeadPresetEntry> categoryEntries : categoryMap.values()) {
			categoryEntries.sort(Comparator.comparing(HeadPresetEntry::name, String.CASE_INSENSITIVE_ORDER));
		}
		byCategory = Map.copyOf(categoryMap);
		byId = Map.copyOf(idMap);
		allEntries = List.copyOf(entries);
	}

	private static HeadPresetEntry toEntry(RawHead rawHead) {
		if (rawHead == null || rawHead.id <= 0 || rawHead.texture == null || rawHead.texture.isBlank()) {
			return null;
		}
		String base64 = HeadTextureEncoding.toBase64(rawHead.texture);
		if (base64.isBlank()) {
			return null;
		}
		String name = rawHead.name == null || rawHead.name.isBlank() ? String.valueOf(rawHead.id) : rawHead.name.trim();
		String category = HeadPresetCategories.fromJsonCategory(rawHead.category);
		return new HeadPresetEntry(String.valueOf(rawHead.id), name, category, base64);
	}

	private static boolean matchesQuery(HeadPresetEntry entry, String query) {
		return entry.name().toLowerCase(Locale.ROOT).contains(query) || entry.id().contains(query);
	}

	private static final class RawHead {
		int id;
		String name;
		String texture;
		String category;
	}
}
