package com.hologrammenu.npc;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class NpcPlacementHandler {
	private NpcPlacementHandler() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			if (NpcPlacementMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
	}
}
