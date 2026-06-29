package com.hologrammenu.hologram;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class HologramPlacementHandler {
	private HologramPlacementHandler() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			if (HologramPlacementMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			if (HologramPlacementMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
	}
}
