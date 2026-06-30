package com.serverutilities.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public final class NpcParticleHandler {
	private NpcParticleHandler() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(NpcParticleHandler::tick);
	}

	private static void tick(MinecraftServer server) {
		long gameTime = server.overworld().getGameTime();
		for (ServerLevel level : server.getAllLevels()) {
			Set<Integer> processed = new HashSet<>();
			for (ServerPlayer player : level.players()) {
				AABB box = player.getBoundingBox().inflate(64.0D);
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, NpcHelper::isNpc)) {
					if (!processed.add(entity.getId())) {
						continue;
					}
					NpcParticleEffects.tickNpc(level, entity, NpcConfig.read(entity), gameTime);
				}
			}
		}
	}
}
