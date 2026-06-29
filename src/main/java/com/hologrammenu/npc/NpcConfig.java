package com.hologrammenu.npc;

import com.hologrammenu.network.ModPackets;
import com.hologrammenu.storage.StorageMenuSizes;
import net.minecraft.world.entity.LivingEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record NpcConfig(
	boolean headFollowEnabled,
	float headFollowRadius,
	String dialogue,
	boolean containerEnabled,
	int containerSize,
	boolean particleEffectEnabled,
	String particleEffectId
) {
	public static final float DEFAULT_HEAD_RADIUS = 4.0F;
	public static final float MIN_HEAD_RADIUS = 1.0F;
	public static final float MAX_HEAD_RADIUS = 16.0F;

	private static final String HEAD_FOLLOW_PREFIX = "hologrammenu:npc_head_follow:";
	private static final String HEAD_RADIUS_PREFIX = "hologrammenu:npc_head_radius:";
	private static final String DIALOGUE_PREFIX = "hologrammenu:npc_dialogue:";
	private static final String CONTAINER_ENABLED_PREFIX = "hologrammenu:npc_container_enabled:";
	private static final String CONTAINER_SIZE_PREFIX = "hologrammenu:npc_container_size:";
	private static final String PARTICLE_ENABLED_PREFIX = "hologrammenu:npc_particle_enabled:";
	private static final String PARTICLE_ID_PREFIX = "hologrammenu:npc_particle_id:";

	public static NpcConfig defaults() {
		return new NpcConfig(false, DEFAULT_HEAD_RADIUS, "", false, StorageMenuSizes.SINGLE_CHEST, false, "");
	}

	public static NpcConfig read(LivingEntity entity) {
		boolean headFollow = readBooleanTag(entity, HEAD_FOLLOW_PREFIX, false);
		float radius = readFloatTag(entity, HEAD_RADIUS_PREFIX, DEFAULT_HEAD_RADIUS);
		String dialogue = readDialogueTag(entity);
		boolean containerEnabled = readBooleanTag(entity, CONTAINER_ENABLED_PREFIX, false);
		int containerSize = readIntTag(entity, CONTAINER_SIZE_PREFIX, StorageMenuSizes.SINGLE_CHEST);
		boolean particleEffectEnabled = readBooleanTag(entity, PARTICLE_ENABLED_PREFIX, false);
		String particleEffectId = readStringTag(entity, PARTICLE_ID_PREFIX, "");
		return new NpcConfig(
			headFollow,
			clampRadius(radius),
			dialogue,
			containerEnabled,
			normalizeContainerSize(containerSize),
			particleEffectEnabled,
			particleEffectId == null ? "" : particleEffectId
		);
	}

	public void write(LivingEntity entity) {
		writeBooleanTag(entity, HEAD_FOLLOW_PREFIX, headFollowEnabled);
		writeFloatTag(entity, HEAD_RADIUS_PREFIX, clampRadius(headFollowRadius));
		writeDialogueTag(entity, dialogue == null ? "" : dialogue);
		writeBooleanTag(entity, CONTAINER_ENABLED_PREFIX, containerEnabled);
		writeIntTag(entity, CONTAINER_SIZE_PREFIX, normalizeContainerSize(containerSize));
		writeBooleanTag(entity, PARTICLE_ENABLED_PREFIX, particleEffectEnabled);
		writeStringTag(entity, PARTICLE_ID_PREFIX, particleEffectId == null ? "" : particleEffectId);
	}

	public static float clampRadius(float value) {
		return Math.clamp(value, MIN_HEAD_RADIUS, MAX_HEAD_RADIUS);
	}

	public static int normalizeContainerSize(int size) {
		return size == StorageMenuSizes.DOUBLE_CHEST ? StorageMenuSizes.DOUBLE_CHEST : StorageMenuSizes.SINGLE_CHEST;
	}

	public static NpcConfig fromPayload(ModPackets.NpcConfigPayload payload) {
		return new NpcConfig(
			payload.headFollowEnabled(),
			clampRadius(payload.headFollowRadius()),
			payload.dialogue() == null ? "" : payload.dialogue(),
			payload.containerEnabled(),
			normalizeContainerSize(payload.containerSize()),
			payload.particleEffectEnabled(),
			payload.particleEffectId() == null ? "" : payload.particleEffectId()
		);
	}

	public ModPackets.NpcConfigPayload toPayload(int entityId) {
		return new ModPackets.NpcConfigPayload(
			entityId,
			headFollowEnabled,
			clampRadius(headFollowRadius),
			dialogue == null ? "" : dialogue,
			containerEnabled,
			normalizeContainerSize(containerSize),
			particleEffectEnabled,
			particleEffectId == null ? "" : particleEffectId
		);
	}

	private static String readStringTag(LivingEntity entity, String prefix, String defaultValue) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(prefix)) {
				return tag.substring(prefix.length());
			}
		}
		return defaultValue;
	}

	private static void writeStringTag(LivingEntity entity, String prefix, String value) {
		entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
		if (value == null || value.isBlank()) {
			return;
		}
		entity.addTag(prefix + value);
	}

	private static boolean readBooleanTag(LivingEntity entity, String prefix, boolean defaultValue) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(prefix)) {
				return Boolean.parseBoolean(tag.substring(prefix.length()));
			}
		}
		return defaultValue;
	}

	private static float readFloatTag(LivingEntity entity, String prefix, float defaultValue) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(prefix)) {
				try {
					return Float.parseFloat(tag.substring(prefix.length()));
				} catch (NumberFormatException ignored) {
					return defaultValue;
				}
			}
		}
		return defaultValue;
	}

	private static int readIntTag(LivingEntity entity, String prefix, int defaultValue) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(prefix)) {
				try {
					return Integer.parseInt(tag.substring(prefix.length()));
				} catch (NumberFormatException ignored) {
					return defaultValue;
				}
			}
		}
		return defaultValue;
	}

	private static String readDialogueTag(LivingEntity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(DIALOGUE_PREFIX)) {
				String encoded = tag.substring(DIALOGUE_PREFIX.length());
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
		return "";
	}

	private static void writeBooleanTag(LivingEntity entity, String prefix, boolean value) {
		entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
		entity.addTag(prefix + value);
	}

	private static void writeFloatTag(LivingEntity entity, String prefix, float value) {
		entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
		entity.addTag(prefix + value);
	}

	private static void writeIntTag(LivingEntity entity, String prefix, int value) {
		entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
		entity.addTag(prefix + value);
	}

	private static void writeDialogueTag(LivingEntity entity, String dialogue) {
		entity.entityTags().removeIf(tag -> tag.startsWith(DIALOGUE_PREFIX));
		if (dialogue == null || dialogue.isBlank()) {
			return;
		}
		String encoded = Base64.getEncoder().encodeToString(dialogue.getBytes(StandardCharsets.UTF_8));
		entity.addTag(DIALOGUE_PREFIX + encoded);
	}
}
