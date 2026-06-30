package com.serverutilities.npc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class NpcHologramStack {
	public static final String KIND_NAME = "name";
	public static final String KIND_HOLOGRAM = "hologram";
	public static final int MAX_ENTRIES = 8;

	private static final Gson GSON = new Gson();
	private static final Type ENTRY_LIST_TYPE = new TypeToken<List<Entry>>() {}.getType();
	private static final String STACK_TAG_PREFIX = "serverutilities:npc_holo_stack:";
	private static final String STYLED_NAME_TAG_PREFIX = "serverutilities:npc_styled_name:";

	public record Entry(String kind, String text, float scale) {
		public Entry {
			kind = kind == null ? KIND_HOLOGRAM : kind;
			text = text == null ? "" : text;
			scale = NpcHologramStack.clampScale(scale);
		}

		public boolean isName() {
			return KIND_NAME.equals(kind);
		}

		public static Entry name(String text) {
			return new Entry(KIND_NAME, text, 1.0F);
		}

		public static Entry hologram(String text) {
			return new Entry(KIND_HOLOGRAM, text, 1.0F);
		}
	}

	private NpcHologramStack() {
	}

	public static List<Entry> defaults(String nameText) {
		return new ArrayList<>(List.of(Entry.name(nameText == null ? "" : nameText)));
	}

	public static List<Entry> read(LivingEntity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(STACK_TAG_PREFIX)) {
				String encoded = tag.substring(STACK_TAG_PREFIX.length());
				if (encoded.isEmpty()) {
					return defaults(readStyledName(entity));
				}
				try {
					String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
					List<Entry> entries = GSON.fromJson(json, ENTRY_LIST_TYPE);
					if (entries == null || entries.isEmpty()) {
						return defaults(readStyledName(entity));
					}
					return normalize(entries, readStyledName(entity));
				} catch (RuntimeException ignored) {
					return defaults(readStyledName(entity));
				}
			}
		}
		return defaults(readStyledName(entity));
	}

	public static void write(LivingEntity entity, List<Entry> entries) {
		entity.entityTags().removeIf(tag -> tag.startsWith(STACK_TAG_PREFIX));
		List<Entry> normalized = normalize(entries, readStyledName(entity));
		if (normalized.size() <= 1 && normalized.getFirst().isName() && normalized.getFirst().text.isBlank()) {
			return;
		}
		String json = GSON.toJson(normalized);
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		entity.addTag(STACK_TAG_PREFIX + encoded);
	}

	public static String readStyledName(LivingEntity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(STYLED_NAME_TAG_PREFIX)) {
				String encoded = tag.substring(STYLED_NAME_TAG_PREFIX.length());
				if (encoded.isEmpty()) {
					return "";
				}
				try {
					return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
				} catch (IllegalArgumentException ignored) {
					return encoded;
				}
			}
		}
		return entity.getCustomName() != null ? entity.getCustomName().getString() : "";
	}

	public static void writeStyledName(LivingEntity entity, String serialized) {
		entity.entityTags().removeIf(tag -> tag.startsWith(STYLED_NAME_TAG_PREFIX));
		if (serialized == null || serialized.isBlank()) {
			return;
		}
		String encoded = Base64.getEncoder().encodeToString(serialized.getBytes(StandardCharsets.UTF_8));
		entity.addTag(STYLED_NAME_TAG_PREFIX + encoded);
	}

	public static String serialize(List<Entry> entries) {
		return GSON.toJson(normalize(entries, ""));
	}

	public static List<Entry> deserialize(String json) {
		if (json == null || json.isBlank()) {
			return defaults("");
		}
		try {
			List<Entry> entries = GSON.fromJson(json, ENTRY_LIST_TYPE);
			return normalize(entries == null ? List.of() : entries, "");
		} catch (RuntimeException ignored) {
			return defaults("");
		}
	}

	public static List<Entry> withNameText(List<Entry> entries, String nameText) {
		List<Entry> copy = new ArrayList<>(entries);
		boolean found = false;
		for (int index = 0; index < copy.size(); index++) {
			Entry entry = copy.get(index);
			if (entry.isName()) {
				copy.set(index, new Entry(KIND_NAME, nameText == null ? "" : nameText, entry.scale()));
				found = true;
				break;
			}
		}
		if (!found) {
			int insertAt = Math.min(1, copy.size());
			copy.add(insertAt, Entry.name(nameText == null ? "" : nameText));
		}
		return normalize(copy, nameText);
	}

	public static boolean hasExtraHolograms(List<Entry> entries) {
		return entries.stream().anyMatch(entry -> !entry.isName());
	}

	public static float clampScale(float scale) {
		return com.serverutilities.hologram.HologramScale.clamp(scale);
	}

	private static List<Entry> normalize(List<Entry> entries, String fallbackName) {
		List<Entry> normalized = new ArrayList<>();
		boolean hasName = false;
		for (Entry entry : entries) {
			if (entry.isName()) {
				if (!hasName) {
					normalized.add(new Entry(KIND_NAME, entry.text(), entry.scale()));
					hasName = true;
				}
				continue;
			}
			if (normalized.size() >= MAX_ENTRIES) {
				break;
			}
			normalized.add(new Entry(KIND_HOLOGRAM, entry.text(), entry.scale()));
		}
		if (!hasName) {
			int insertAt = Math.min(1, normalized.size());
			normalized.add(insertAt, Entry.name(fallbackName == null ? "" : fallbackName));
		}
		while (normalized.size() > MAX_ENTRIES) {
			for (int index = normalized.size() - 1; index >= 0; index--) {
				if (!normalized.get(index).isName()) {
					normalized.remove(index);
					break;
				}
			}
		}
		return normalized;
	}
}
