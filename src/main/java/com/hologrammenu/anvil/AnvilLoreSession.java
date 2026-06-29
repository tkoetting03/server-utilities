package com.hologrammenu.anvil;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AnvilLoreSession {
	private static final Map<UUID, List<String>> PENDING = new HashMap<>();

	private AnvilLoreSession() {
	}

	public static void set(ServerPlayer player, List<String> lines) {
		PENDING.put(player.getUUID(), lines == null ? List.of() : List.copyOf(lines));
	}

	public static boolean hasPending(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	public static List<String> get(ServerPlayer player) {
		return PENDING.get(player.getUUID());
	}

	public static void clear(ServerPlayer player) {
		PENDING.remove(player.getUUID());
	}

	public static List<String> copyOrEmpty(ServerPlayer player) {
		List<String> lines = PENDING.get(player.getUUID());
		return lines == null ? List.of() : new ArrayList<>(lines);
	}
}
