package com.hologrammenu.client.hologram;

import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.hologram.HologramHelper;
import com.hologrammenu.network.ModPackets;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;

public final class HologramClientInteractions {
	private HologramClientInteractions() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.isClientSide()) {
				return InteractionResult.PASS;
			}
			if (isOtherModeActive()) {
				return InteractionResult.PASS;
			}
			if (!ClientSettings.placementModeEnabled) {
				return InteractionResult.PASS;
			}
			if (level.getBlockState(hitResult.getBlockPos()).isAir()) {
				return InteractionResult.PASS;
			}
			return sendPlacementPacket();
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide()) {
				return InteractionResult.PASS;
			}

			if (isOtherModeActive()) {
				return InteractionResult.PASS;
			}

			if (!ClientSettings.placementModeEnabled) {
				return InteractionResult.PASS;
			}

			HitResult hit = player.pick(HologramHelper.WAND_MAX_DISTANCE, 1.0F, false);
			if (hit.getType() == HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}

			return sendPlacementPacket();
		});
	}

	private static boolean isOtherModeActive() {
		return ClientSettings.npcPlacementModeEnabled
			|| ClientSettings.npcEditModeEnabled
			|| ClientSettings.hologramEditModeEnabled;
	}

	private static InteractionResult sendPlacementPacket() {
		ClientPlayNetworking.send(new ModPackets.HologramPlacePayload(ClientSettings.defaultPlacementText));
		return InteractionResult.SUCCESS;
	}
}
