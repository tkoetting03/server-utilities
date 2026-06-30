package com.serverutilities.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class NpcHologramLabelHandler {
	private NpcHologramLabelHandler() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(NpcHologramLabelHandler::tick);
	}

	private static void tick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			NpcHologramLabels.tickTracked(level);
		}
	}
}
