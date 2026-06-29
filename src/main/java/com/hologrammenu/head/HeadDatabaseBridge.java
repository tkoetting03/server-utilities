package com.hologrammenu.head;

import com.hologrammenu.HologramMenuMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HeadDatabaseBridge {
	private static final String API_CLASS = "me.arcaniax.hdb.api.HeadDatabaseAPI";
	private static final String CATEGORY_CLASS = "me.arcaniax.hdb.enums.CategoryEnum";
	private static final String HEAD_CLASS = "me.arcaniax.hdb.object.head.Head";
	private static final String[] BROWSE_CATEGORIES = HeadPresetCategories.PLUGIN;

	private static Boolean available;

	private HeadDatabaseBridge() {
	}

	public static boolean isAvailable() {
		if (available != null) {
			return available;
		}
		try {
			Class.forName(API_CLASS, false, HeadDatabaseBridge.class.getClassLoader());
			Object api = createApi();
			available = api != null;
		} catch (ReflectiveOperationException exception) {
			available = false;
		}
		return available;
	}

	public static List<HeadPresetEntry> list(String category, String query, int page, int pageSize) {
		if (!isAvailable()) {
			return List.of();
		}
		try {
			Object api = createApi();
			if (api == null) {
				return List.of();
			}
			List<HeadPresetEntry> entries = new ArrayList<>();
			String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			if (normalizedQuery.isEmpty()) {
				Object categoryEnum = resolveCategory(category);
				if (categoryEnum == null) {
					categoryEnum = resolveCategory(BROWSE_CATEGORIES[0]);
				}
				if (categoryEnum != null) {
					entries.addAll(readCategory(api, categoryEnum));
				}
			} else {
				for (String categoryName : BROWSE_CATEGORIES) {
					Object categoryEnum = resolveCategory(categoryName);
					if (categoryEnum == null) {
						continue;
					}
					for (HeadPresetEntry entry : readCategory(api, categoryEnum)) {
						if (matchesQuery(entry, normalizedQuery)) {
							entries.add(entry);
						}
					}
				}
			}
			entries.sort(Comparator.comparing(HeadPresetEntry::name, String.CASE_INSENSITIVE_ORDER));
			int start = Math.max(0, page) * pageSize;
			if (start >= entries.size()) {
				return List.of();
			}
			int end = Math.min(entries.size(), start + pageSize);
			return List.copyOf(entries.subList(start, end));
		} catch (ReflectiveOperationException exception) {
			HologramMenuMod.LOGGER.warn("Failed to read Head Database entries", exception);
			return List.of();
		}
	}

	public static int count(String category, String query) {
		if (!isAvailable()) {
			return 0;
		}
		try {
			Object api = createApi();
			if (api == null) {
				return 0;
			}
			String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			if (normalizedQuery.isEmpty()) {
				Object categoryEnum = resolveCategory(category);
				if (categoryEnum == null) {
					categoryEnum = resolveCategory(BROWSE_CATEGORIES[0]);
				}
				return categoryEnum == null ? 0 : readCategory(api, categoryEnum).size();
			}
			int total = 0;
			for (String categoryName : BROWSE_CATEGORIES) {
				Object categoryEnum = resolveCategory(categoryName);
				if (categoryEnum == null) {
					continue;
				}
				for (HeadPresetEntry entry : readCategory(api, categoryEnum)) {
					if (matchesQuery(entry, normalizedQuery)) {
						total++;
					}
				}
			}
			return total;
		} catch (ReflectiveOperationException exception) {
			HologramMenuMod.LOGGER.warn("Failed to count Head Database entries", exception);
			return 0;
		}
	}

	public static Optional<HeadPresetEntry> getEntry(String id) {
		if (!isAvailable() || id == null || id.isBlank()) {
			return Optional.empty();
		}
		try {
			Object api = createApi();
			if (api == null) {
				return Optional.empty();
			}
			String trimmedId = id.trim();
			Method method = api.getClass().getMethod("getBase64", String.class);
			Object value = method.invoke(api, trimmedId);
			if (!(value instanceof String base64) || base64.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new HeadPresetEntry(trimmedId, trimmedId, "", base64));
		} catch (ReflectiveOperationException exception) {
			HologramMenuMod.LOGGER.warn("Failed to read Head Database entry {}", id, exception);
			return Optional.empty();
		}
	}

	public static Optional<String> getBase64(String id) {
		return getEntry(id).map(HeadPresetEntry::base64);
	}

	public static String[] browseCategories() {
		return BROWSE_CATEGORIES.clone();
	}

	private static boolean matchesQuery(HeadPresetEntry entry, String query) {
		if (entry.name().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		return entry.id().contains(query);
	}

	private static Object createApi() throws ReflectiveOperationException {
		Class<?> apiClass = Class.forName(API_CLASS);
		return apiClass.getConstructor().newInstance();
	}

	private static Object resolveCategory(String category) throws ReflectiveOperationException {
		if (category == null || category.isBlank()) {
			return null;
		}
		Class<?> enumClass = Class.forName(CATEGORY_CLASS);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Object value = Enum.valueOf((Class<Enum>) enumClass, category.trim().toUpperCase(Locale.ROOT));
		return value;
	}

	@SuppressWarnings("unchecked")
	private static List<HeadPresetEntry> readCategory(Object api, Object categoryEnum) throws ReflectiveOperationException {
		Method getHeads = api.getClass().getMethod("getHeads", categoryEnum.getClass());
		Object result = getHeads.invoke(api, categoryEnum);
		if (!(result instanceof List<?> heads)) {
			return List.of();
		}
		String categoryName = categoryEnum.toString();
		List<HeadPresetEntry> entries = new ArrayList<>(heads.size());
		for (Object head : heads) {
			HeadPresetEntry entry = toEntry(head, categoryName);
			if (entry != null) {
				entries.add(entry);
			}
		}
		return entries;
	}

	private static HeadPresetEntry toEntry(Object head, String category) throws ReflectiveOperationException {
		if (head == null) {
			return null;
		}
		Class<?> headClass = Class.forName(HEAD_CLASS);
		if (!headClass.isInstance(head)) {
			return null;
		}
		String id = readStringField(head, "id");
		String name = readStringField(head, "name");
		String base64 = readStringField(head, "b64");
		if (id == null || id.isBlank() || base64 == null || base64.isBlank()) {
			return null;
		}
		if (name == null || name.isBlank()) {
			name = id;
		}
		return new HeadPresetEntry(id, name, category, base64);
	}

	private static String readStringField(Object target, String fieldName) throws ReflectiveOperationException {
		Field field = target.getClass().getField(fieldName);
		Object value = field.get(target);
		return value instanceof String string ? string : null;
	}
}
