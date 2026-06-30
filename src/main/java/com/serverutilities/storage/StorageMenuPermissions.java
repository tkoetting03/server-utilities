package com.serverutilities.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class StorageMenuPermissions {
	private StorageMenuPermissions() {
	}

	public static boolean canEdit(ServerPlayer player) {
		if (player.getAbilities().instabuild) {
			return true;
		}
		if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return true;
		}
		MinecraftServer server = player.level().getServer();
		if (server.isSingleplayer()) {
			return true;
		}
		return !server.isDedicatedServer();
	}
}
