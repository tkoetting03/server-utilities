package com.hologrammenu.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class NpcHeadFollowHandler {
	private NpcHeadFollowHandler() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(NpcHeadFollowHandler::tick);
	}

	private static void tick(MinecraftServer server) {
		server.getAllLevels().forEach(NpcHelper::tickHeadFollow);
	}
}
