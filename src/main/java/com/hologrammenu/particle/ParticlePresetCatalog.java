package com.hologrammenu.particle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ParticlePresetCatalog {
	public static final int PAGE_SIZE = 54;

	private static final List<ParticlePresetEntry> ENTRIES = buildEntries();
	private static final Map<String, List<ParticlePresetEntry>> BY_CATEGORY = ENTRIES.stream()
		.collect(Collectors.groupingBy(ParticlePresetEntry::category));

	private ParticlePresetCatalog() {
	}

	public static List<ParticlePresetEntry> list(String category, String query, int page, int pageSize) {
		String normalizedCategory = ParticlePresetCategories.normalize(category);
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<ParticlePresetEntry> source = new ArrayList<>(BY_CATEGORY.getOrDefault(normalizedCategory, List.of()));
		if (!normalizedQuery.isEmpty()) {
			source = source.stream()
				.filter(entry -> matchesQuery(entry, normalizedQuery))
				.collect(Collectors.toCollection(ArrayList::new));
		}
		source.sort(Comparator.comparing(ParticlePresetEntry::name, String.CASE_INSENSITIVE_ORDER));
		int start = Math.max(0, page) * pageSize;
		if (start >= source.size()) {
			return List.of();
		}
		int end = Math.min(source.size(), start + pageSize);
		return List.copyOf(source.subList(start, end));
	}

	public static int count(String category, String query) {
		String normalizedCategory = ParticlePresetCategories.normalize(category);
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		List<ParticlePresetEntry> source = BY_CATEGORY.getOrDefault(normalizedCategory, List.of());
		if (normalizedQuery.isEmpty()) {
			return source.size();
		}
		int total = 0;
		for (ParticlePresetEntry entry : source) {
			if (matchesQuery(entry, normalizedQuery)) {
				total++;
			}
		}
		return total;
	}

	public static Optional<ParticlePresetEntry> get(String id) {
		if (id == null || id.isBlank()) {
			return Optional.of(noneEntry());
		}
		String normalizedId = id.trim();
		for (ParticlePresetEntry entry : ENTRIES) {
			if (normalizedId.equals(entry.id())) {
				return Optional.of(entry);
			}
		}
		return Optional.empty();
	}

	public static ParticlePresetEntry noneEntry() {
		return new ParticlePresetEntry(ParticlePresetEntry.NONE_ID, "None", ParticlePresetCategories.NONE, 0xFF4A4A4A);
	}

	private static boolean matchesQuery(ParticlePresetEntry entry, String query) {
		return entry.name().toLowerCase(Locale.ROOT).contains(query)
			|| entry.id().toLowerCase(Locale.ROOT).contains(query);
	}

	private static List<ParticlePresetEntry> buildEntries() {
		List<ParticlePresetEntry> entries = new ArrayList<>();
		entries.add(noneEntry());
		entries.addAll(category(ParticlePresetCategories.AMBIENT,
			entry(ParticlePresetCategories.AMBIENT, "cloud", "Cloud", 0xFFB0B0B0),
			entry(ParticlePresetCategories.AMBIENT, "smoke", "Smoke", 0xFF6E6E6E),
			entry(ParticlePresetCategories.AMBIENT, "white_smoke", "White Smoke", 0xFFE0E0E0),
			entry(ParticlePresetCategories.AMBIENT, "large_smoke", "Large Smoke", 0xFF5A5A5A),
			entry(ParticlePresetCategories.AMBIENT, "poof", "Poof", 0xFFD8D8D8),
			entry(ParticlePresetCategories.AMBIENT, "portal", "Portal", 0xFF9B59FF),
			entry(ParticlePresetCategories.AMBIENT, "rain", "Rain", 0xFF4A90E2),
			entry(ParticlePresetCategories.AMBIENT, "underwater", "Underwater", 0xFF2F6FB3),
			entry(ParticlePresetCategories.AMBIENT, "bubble", "Bubble", 0xFF8FD3FF),
			entry(ParticlePresetCategories.AMBIENT, "bubble_pop", "Bubble Pop", 0xFF9AD9FF),
			entry(ParticlePresetCategories.AMBIENT, "nautilus", "Nautilus", 0xFF6EC6FF)
		));
		entries.addAll(category(ParticlePresetCategories.MAGIC,
			entry(ParticlePresetCategories.MAGIC, "enchant", "Enchant", 0xFF7C4DFF),
			entry(ParticlePresetCategories.MAGIC, "witch", "Witch", 0xFF6B3FA0),
			entry(ParticlePresetCategories.MAGIC, "totem_of_undying", "Totem", 0xFFFFD54F),
			entry(ParticlePresetCategories.MAGIC, "soul", "Soul", 0xFF4EC5D4),
			entry(ParticlePresetCategories.MAGIC, "sculk_soul", "Sculk Soul", 0xFF1FA3A3),
			entry(ParticlePresetCategories.MAGIC, "end_rod", "End Rod", 0xFFFFE08A),
			entry(ParticlePresetCategories.MAGIC, "enchanted_hit", "Enchanted Hit", 0xFF9FE870),
			entry(ParticlePresetCategories.MAGIC, "flash", "Flash", 0xFFFFFFFF),
			entry(ParticlePresetCategories.MAGIC, "sonic_boom", "Sonic Boom", 0xFF8AD4FF)
		));
		entries.addAll(category(ParticlePresetCategories.NATURE,
			entry(ParticlePresetCategories.NATURE, "happy_villager", "Happy Villager", 0xFF7CFC00),
			entry(ParticlePresetCategories.NATURE, "heart", "Heart", 0xFFFF4D6D),
			entry(ParticlePresetCategories.NATURE, "cherry_leaves", "Cherry Leaves", 0xFFFFB7C5),
			entry(ParticlePresetCategories.NATURE, "pale_oak_leaves", "Pale Oak Leaves", 0xFFCED8B6),
			entry(ParticlePresetCategories.NATURE, "mycelium", "Mycelium", 0xFF8A5CCF),
			entry(ParticlePresetCategories.NATURE, "composter", "Composter", 0xFF8B5A2B),
			entry(ParticlePresetCategories.NATURE, "falling_water", "Falling Water", 0xFF4A90E2),
			entry(ParticlePresetCategories.NATURE, "dripping_water", "Dripping Water", 0xFF3D7EBF),
			entry(ParticlePresetCategories.NATURE, "spore_blossom_air", "Spore Blossom", 0xFFFF8FD0)
		));
		entries.addAll(category(ParticlePresetCategories.FIRE,
			entry(ParticlePresetCategories.FIRE, "flame", "Flame", 0xFFFF8C00),
			entry(ParticlePresetCategories.FIRE, "small_flame", "Small Flame", 0xFFFFA64D),
			entry(ParticlePresetCategories.FIRE, "soul_fire_flame", "Soul Fire", 0xFF4EC5D4),
			entry(ParticlePresetCategories.FIRE, "lava", "Lava", 0xFFFF6A00),
			entry(ParticlePresetCategories.FIRE, "campfire_cosy_smoke", "Campfire Smoke", 0xFF8A8A8A),
			entry(ParticlePresetCategories.FIRE, "campfire_signal_smoke", "Signal Smoke", 0xFF9E9E9E),
			entry(ParticlePresetCategories.FIRE, "copper_fire_flame", "Copper Flame", 0xFFFF9A3C),
			entry(ParticlePresetCategories.FIRE, "glow", "Glow", 0xFFFFE066)
		));
		entries.addAll(category(ParticlePresetCategories.MOB,
			entry(ParticlePresetCategories.MOB, "angry_villager", "Angry Villager", 0xFF9B2C2C),
			entry(ParticlePresetCategories.MOB, "squid_ink", "Squid Ink", 0xFF2E2E2E),
			entry(ParticlePresetCategories.MOB, "sneeze", "Sneeze", 0xFFE8F4FF),
			entry(ParticlePresetCategories.MOB, "spit", "Spit", 0xFF9ACD32),
			entry(ParticlePresetCategories.MOB, "infested", "Infested", 0xFF5E8B3A),
			entry(ParticlePresetCategories.MOB, "crit", "Critical Hit", 0xFFFFE066),
			entry(ParticlePresetCategories.MOB, "damage_indicator", "Damage", 0xFFFF4D4D),
			entry(ParticlePresetCategories.MOB, "sweep_attack", "Sweep Attack", 0xFFB0B0B0),
			entry(ParticlePresetCategories.MOB, "dolphin", "Dolphin", 0xFF4EC5D4),
			entry(ParticlePresetCategories.MOB, "note", "Note", 0xFFFF66CC)
		));
		return List.copyOf(entries);
	}

	private static List<ParticlePresetEntry> category(String category, ParticlePresetEntry... values) {
		return List.of(values);
	}

	private static ParticlePresetEntry entry(String category, String id, String name, int previewColor) {
		return new ParticlePresetEntry(id, name, category, previewColor);
	}
}
