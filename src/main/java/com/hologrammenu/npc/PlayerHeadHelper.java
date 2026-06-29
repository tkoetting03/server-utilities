package com.hologrammenu.npc;

import com.hologrammenu.head.HeadPresetCatalog;
import com.hologrammenu.head.HeadPresetIds;
import com.hologrammenu.head.HeadProfileHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;

import java.util.Optional;

public final class PlayerHeadHelper {
	private PlayerHeadHelper() {
	}

	public static boolean give(ServerPlayer player, String profileName) {
		return give(player, profileName, "");
	}

	public static boolean give(ServerPlayer player, String profileName, String headDatabaseId) {
		return give(player, profileName, headDatabaseId, "");
	}

	public static boolean give(ServerPlayer player, String profileName, String headDatabaseId, String headDatabaseBase64) {
		if (headDatabaseId != null && !headDatabaseId.isBlank()) {
			return giveHeadPreset(player, headDatabaseId.trim(), profileName, headDatabaseBase64);
		}
		if (profileName == null || profileName.isBlank()) {
			return false;
		}
		String trimmed = profileName.trim();
		if (HeadPresetIds.parseId(trimmed).isPresent()) {
			return giveHeadPreset(player, HeadPresetIds.parseId(trimmed).get(), trimmed, "");
		}
		return giveProfile(player, ResolvableProfile.createUnresolved(trimmed), trimmed);
	}

	private static boolean giveHeadPreset(ServerPlayer player, String headId, String displayName, String providedBase64) {
		String base64 = providedBase64 == null ? "" : providedBase64.trim();
		if (base64.isBlank()) {
			base64 = HeadPresetCatalog.getBase64(headId).orElse("");
		}
		if (base64.isBlank()) {
			player.sendSystemMessage(Component.translatable("hud.hologrammenu.head_presets.not_found", headId));
			return false;
		}
		String label = displayName == null || displayName.isBlank() ? headId : displayName.trim();
		Optional<ResolvableProfile> profile = HeadProfileHelper.fromTextureBase64(label, base64);
		if (profile.isEmpty()) {
			player.sendSystemMessage(Component.translatable("hud.hologrammenu.head_presets.not_found", headId));
			return false;
		}
		return giveProfile(player, profile.get(), label);
	}

	private static boolean giveProfile(ServerPlayer player, ResolvableProfile profile, String messageLabel) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, profile);

		if (!player.getInventory().add(stack)) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) {
				drop.setNoPickUpDelay();
				drop.setTarget(player.getUUID());
			}
		}

		player.sendSystemMessage(Component.translatable("hud.hologrammenu.player_head.given", messageLabel));
		return true;
	}
}
