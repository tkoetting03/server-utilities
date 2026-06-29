package com.hologrammenu.hologram;

import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class HologramSync {
	private HologramSync() {
	}

	public static void track(ServerLevel level, Display.TextDisplay display) {
		ModPackets.HologramTrackPayload payload = payload(display);
		java.util.Set<ServerPlayer> recipients = new java.util.HashSet<>(PlayerLookup.tracking(display));
		recipients.addAll(PlayerLookup.around(level, display.position(), 64.0D));
		for (ServerPlayer player : recipients) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void untrack(ServerLevel level, Entity entity) {
		untrack(level, entity.getId(), entity.position());
	}

	public static void untrack(ServerLevel level, int entityId, Vec3 position) {
		ModPackets.HologramUntrackPayload payload = new ModPackets.HologramUntrackPayload(entityId);
		for (ServerPlayer player : PlayerLookup.around(level, position, 64.0D)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncPlayer(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		List<ModPackets.HologramTrackPayload> holograms = new java.util.ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof Display.TextDisplay display && HologramHelper.isHologram(display)) {
				holograms.add(payload(display));
			}
		}
		ServerPlayNetworking.send(player, new ModPackets.HologramSyncPayload(holograms));
	}

	private static ModPackets.HologramTrackPayload payload(Display.TextDisplay display) {
		UUID groupId = HologramLineStack.groupId(display);
		return new ModPackets.HologramTrackPayload(
			display.getId(),
			groupId == null ? "" : groupId.toString(),
			HologramLineStack.lineIndex(display)
		);
	}
}
