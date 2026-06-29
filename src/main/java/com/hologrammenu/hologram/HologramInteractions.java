package com.hologrammenu.hologram;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class HologramInteractions {
	private HologramInteractions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!HologramHelper.isEditableHologram(entity)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer && HologramEditMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

	}
}
