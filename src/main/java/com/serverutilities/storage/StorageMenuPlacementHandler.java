package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class StorageMenuPlacementHandler {
	private static final double MAX_DISTANCE = 12.0D;

	private StorageMenuPlacementHandler() {
	}

	public static void assign(ServerPlayer player, BlockPos pos) {
		if (!StorageMenuPlacementMode.isActive(player) || !StorageMenuPermissions.canEdit(player)) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		BlockPos target = pos.immutable();
		if (!canAccess(player, target)) {
			return;
		}

		BlockState state = level.getBlockState(target);
		if (state.isAir()) {
			return;
		}

		int containerSize = defaultContainerSize(level, target);
		StorageMenuBlockStore.ensureEnabled(level, target, containerSize);
		StorageMenuOpener.openBlock(player, target);
		StorageMenuInteractions.sendFeedback(player, "screen.serverutilities.storage_menu.assigned");
	}

	private static int defaultContainerSize(ServerLevel level, BlockPos pos) {
		int physical = StorageMenuOpener.resolvePhysicalContainerSize(level, pos);
		return physical > 0 ? physical : StorageMenuSizes.SINGLE_CHEST;
	}

	private static boolean canAccess(ServerPlayer player, BlockPos pos) {
		return player.blockPosition().distSqr(pos) <= MAX_DISTANCE * MAX_DISTANCE;
	}
}
