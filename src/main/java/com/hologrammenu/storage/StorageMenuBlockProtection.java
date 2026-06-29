package com.hologrammenu.storage;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class StorageMenuBlockProtection {
	private StorageMenuBlockProtection() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (world.isClientSide() || !(world instanceof ServerLevel level)) {
				return true;
			}
			return !tryDenyBreak(level, pos, player);
		});
	}

	public static boolean isProtected(ServerLevel level, BlockPos pos) {
		return StorageMenuBlockStore.isInvulnerable(level, pos);
	}

	public static boolean tryDenyBreak(ServerLevel level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
		if (!isProtected(level, pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		level.sendBlockUpdated(pos, state, state, 3);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			serverPlayer.sendSystemMessage(Component.translatable("screen.hologrammenu.storage_menu.unbreakable_block"), true);
		}
		return true;
	}
}
