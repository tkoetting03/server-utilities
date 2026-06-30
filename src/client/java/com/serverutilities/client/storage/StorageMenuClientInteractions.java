package com.serverutilities.client.storage;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.storage.StorageMenuClientPermissions;
import com.serverutilities.network.ModPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class StorageMenuClientInteractions {
	private StorageMenuClientInteractions() {
	}

	public static void register() {
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!ClientSettings.storagePlacementModeEnabled || client.level == null) {
				return false;
			}
			if (!StorageMenuClientPermissions.canEdit()) {
				return false;
			}

			BlockPos target = pickTargetBlock(client);
			if (target == null) {
				return false;
			}

			ClientPlayNetworking.send(new ModPackets.StorageMenuAssignPayload(target));
			StorageMenuAssociatedBlocks.remember(target);
			return true;
		});
	}

	public static void handlePlacementToggle(Minecraft client, boolean enabled) {
		ClientPlayNetworking.send(new ModPackets.SetStoragePlacementModePayload(enabled));
		if (client.player != null) {
			client.player.sendOverlayMessage(
				enabled
					? Component.translatable("hud.serverutilities.storage_placement.enabled")
					: Component.translatable("hud.serverutilities.storage_placement.disabled")
			);
		}
	}

	private static BlockPos pickTargetBlock(Minecraft client) {
		if (client.player == null || client.level == null) {
			return null;
		}

		double reach = client.player.blockInteractionRange();
		HitResult hit = client.player.pick(reach, 0.0F, false);
		if (!(hit instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		if (client.level.getBlockState(hitResult.getBlockPos()).isAir()) {
			return null;
		}
		return hitResult.getBlockPos().immutable();
	}
}
