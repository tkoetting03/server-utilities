package com.serverutilities.client.hologram;

import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.hologram.HologramHelper;
import com.serverutilities.network.ModPackets;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

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
			return sendPlacementPacket(player, hitResult);
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

			return sendPlacementPacket(player, hit);
		});
	}

	private static boolean isOtherModeActive() {
		return ClientSettings.npcPlacementModeEnabled
			|| ClientSettings.npcEditModeEnabled
			|| ClientSettings.hologramEditModeEnabled;
	}

	private static InteractionResult sendPlacementPacket(Player player, HitResult hitResult) {
		var target = buildPlacementTarget(player, hitResult);
		ClientPlayNetworking.send(new ModPackets.HologramPlacePayload(
			ClientSettings.defaultPlacementText,
			target.position(),
			target.blockPos()
		));
		return InteractionResult.SUCCESS;
	}

	private static HologramTarget buildPlacementTarget(Player player, HitResult hitResult) {
		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) hitResult;
			Vec3 offset = new Vec3(
				blockHit.getDirection().getStepX(),
				blockHit.getDirection().getStepY(),
				blockHit.getDirection().getStepZ()
			).scale(0.05D);
			return new HologramTarget(blockHit.getLocation().add(offset), Optional.of(blockHit.getBlockPos()));
		}
		var fallback = HologramHelper.pickPlacementTarget(player, HologramHelper.WAND_MAX_DISTANCE);
		return new HologramTarget(fallback.position(), fallback.blockPos());
	}

	private record HologramTarget(Vec3 position, Optional<BlockPos> blockPos) {
	}
}
