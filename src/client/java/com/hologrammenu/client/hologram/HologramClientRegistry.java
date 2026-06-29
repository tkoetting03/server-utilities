package com.hologrammenu.client.hologram;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import com.hologrammenu.network.ModPackets;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramClientRegistry {
	private static final Map<Integer, Metadata> HOLOGRAMS = new ConcurrentHashMap<>();

	public record Metadata(String groupId, int lineIndex) {
		public Metadata {
			groupId = groupId == null ? "" : groupId;
		}
	}

	private HologramClientRegistry() {
	}

	public static void sync(Collection<ModPackets.HologramTrackPayload> holograms) {
		HOLOGRAMS.clear();
		for (ModPackets.HologramTrackPayload hologram : holograms) {
			track(hologram);
		}
	}

	public static void track(ModPackets.HologramTrackPayload hologram) {
		HOLOGRAMS.put(hologram.entityId(), new Metadata(hologram.groupId(), hologram.lineIndex()));
	}

	public static void untrack(int entityId) {
		HOLOGRAMS.remove(entityId);
	}

	public static void clear() {
		HOLOGRAMS.clear();
	}

	public static java.util.Set<Integer> knownIds() {
		return HOLOGRAMS.keySet();
	}

	public static String groupId(Entity entity) {
		Metadata metadata = entity == null ? null : HOLOGRAMS.get(entity.getId());
		return metadata == null ? "" : metadata.groupId();
	}

	public static int lineIndex(Entity entity) {
		Metadata metadata = entity == null ? null : HOLOGRAMS.get(entity.getId());
		return metadata == null ? 0 : metadata.lineIndex();
	}

	public static boolean isHologram(Entity entity) {
		return entity != null && HOLOGRAMS.containsKey(entity.getId());
	}

	public static boolean isEditableHologram(Entity entity) {
		return entity instanceof Display.TextDisplay && isHologram(entity);
	}
}
