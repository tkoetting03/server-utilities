package com.serverutilities.client.npc;

import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.screen.NpcToolScreen;
import com.serverutilities.client.storage.StorageMenuClientPermissions;
import com.serverutilities.hologram.HologramHelper;
import com.serverutilities.npc.NpcHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NpcClientInteractions {
	private NpcClientInteractions() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.isClientSide() || !ClientSettings.npcPlacementModeEnabled || ClientSettings.npcEditModeEnabled) {
				return InteractionResult.PASS;
			}
			if (!StorageMenuClientPermissions.canEdit()) {
				return InteractionResult.PASS;
			}
			if (level.getBlockState(hitResult.getBlockPos()).isAir()) {
				return InteractionResult.PASS;
			}
			return openPlacementScreen(HologramHelper.pickPlacementPosition(player, NpcHelper.PLACE_MAX_DISTANCE));
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() || !ClientSettings.npcPlacementModeEnabled || ClientSettings.npcEditModeEnabled) {
				return InteractionResult.PASS;
			}
			if (!StorageMenuClientPermissions.canEdit()) {
				return InteractionResult.PASS;
			}

			HitResult hit = player.pick(NpcHelper.PLACE_MAX_DISTANCE, 1.0F, false);
			if (hit.getType() == HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}

			return openPlacementScreen(HologramHelper.pickPlacementPosition(player, NpcHelper.PLACE_MAX_DISTANCE));
		});
	}

	private static InteractionResult openPlacementScreen(Vec3 position) {
		Minecraft client = Minecraft.getInstance();
		client.setScreen(new NpcToolScreen(client.screen, position));
		return InteractionResult.SUCCESS;
	}
}
