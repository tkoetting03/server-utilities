package com.hologrammenu.npc;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcPlacementMode {
	private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();

	private NpcPlacementMode() {
	}

	public static void setActive(ServerPlayer player, boolean active) {
		if (active) {
			ACTIVE.put(player.getUUID(), true);
		} else {
			ACTIVE.remove(player.getUUID());
		}
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.getOrDefault(player.getUUID(), false);
	}

	public static void clear(ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
	}
}
