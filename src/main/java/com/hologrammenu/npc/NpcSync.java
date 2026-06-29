package com.hologrammenu.npc;

import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NpcSync {
	private NpcSync() {
	}

	public static void track(ServerLevel level, LivingEntity npc) {
		ModPackets.NpcTrackPayload payload = new ModPackets.NpcTrackPayload(npc.getId());
		Set<ServerPlayer> recipients = trackingPlayers(level, npc);
		for (ServerPlayer player : recipients) {
			ServerPlayNetworking.send(player, payload);
			sendConfig(player, npc);
		}
	}

	public static void syncConfig(ServerLevel level, LivingEntity npc) {
		ModPackets.NpcConfigPayload payload = configPayload(npc);
		for (ServerPlayer player : trackingPlayers(level, npc)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void untrack(ServerLevel level, Entity entity) {
		untrack(level, entity.getId(), entity.position());
	}

	public static void untrack(ServerLevel level, int entityId, Vec3 position) {
		ModPackets.NpcUntrackPayload payload = new ModPackets.NpcUntrackPayload(entityId);
		for (ServerPlayer player : PlayerLookup.around(level, position, 64.0D)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncPlayer(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		List<Integer> entityIds = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof LivingEntity living && NpcHelper.isNpc(living)) {
				entityIds.add(living.getId());
				sendConfig(player, living);
			}
		}
		ServerPlayNetworking.send(player, new ModPackets.NpcSyncPayload(entityIds));
	}

	private static void sendConfig(ServerPlayer player, LivingEntity npc) {
		ServerPlayNetworking.send(player, configPayload(npc));
	}

	private static ModPackets.NpcConfigPayload configPayload(LivingEntity npc) {
		return NpcConfig.read(npc).toPayload(npc.getId(), NpcHelper.readSkinName(npc));
	}

	private static Set<ServerPlayer> trackingPlayers(ServerLevel level, LivingEntity npc) {
		Set<ServerPlayer> recipients = new HashSet<>(PlayerLookup.tracking(npc));
		recipients.addAll(PlayerLookup.around(level, npc.position(), 64.0D));
		return recipients;
	}
}
