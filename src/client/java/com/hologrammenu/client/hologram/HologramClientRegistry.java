package com.hologrammenu.client.hologram;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramClientRegistry {
	private static final Set<Integer> HOLOGRAM_IDS = ConcurrentHashMap.newKeySet();

	private HologramClientRegistry() {
	}

	public static void sync(Collection<Integer> entityIds) {
		HOLOGRAM_IDS.clear();
		HOLOGRAM_IDS.addAll(entityIds);
	}

	public static void track(int entityId) {
		HOLOGRAM_IDS.add(entityId);
	}

	public static void untrack(int entityId) {
		HOLOGRAM_IDS.remove(entityId);
	}

	public static void clear() {
		HOLOGRAM_IDS.clear();
	}

	public static Set<Integer> knownIds() {
		return HOLOGRAM_IDS;
	}

	public static boolean isHologram(Entity entity) {
		return entity != null && HOLOGRAM_IDS.contains(entity.getId());
	}

	public static boolean isEditableHologram(Entity entity) {
		return entity instanceof Display.TextDisplay && isHologram(entity);
	}
}
