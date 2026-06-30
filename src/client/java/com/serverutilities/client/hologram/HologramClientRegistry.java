package com.serverutilities.client.hologram;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import com.serverutilities.network.ModPackets;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramClientRegistry {
	private static final Map<Integer, Metadata> HOLOGRAMS = new ConcurrentHashMap<>();

	public record Metadata(String groupId, int lineIndex, Optional<BlockPos> blockPos, Optional<UUID> entityUuid) {
		public Metadata {
			groupId = groupId == null ? "" : groupId;
			blockPos = blockPos == null ? Optional.empty() : blockPos.map(BlockPos::immutable);
			entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
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
		HOLOGRAMS.put(hologram.entityId(), new Metadata(hologram.groupId(), hologram.lineIndex(), hologram.blockPos(), hologram.entityUuid()));
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

	public static Optional<BlockPos> associatedBlock(Entity entity) {
		Metadata metadata = entity == null ? null : HOLOGRAMS.get(entity.getId());
		return metadata == null ? Optional.empty() : metadata.blockPos();
	}

	public static Optional<UUID> associatedEntityUuid(Entity entity) {
		Metadata metadata = entity == null ? null : HOLOGRAMS.get(entity.getId());
		return metadata == null ? Optional.empty() : metadata.entityUuid();
	}

	public static boolean isHologram(Entity entity) {
		return entity != null && HOLOGRAMS.containsKey(entity.getId());
	}

	public static boolean isEditableHologram(Entity entity) {
		return entity instanceof Display.TextDisplay && isHologram(entity);
	}
}
