package com.hologrammenu.client.head;

import com.hologrammenu.head.HeadPresetEntry;
import com.hologrammenu.head.HeadProfileHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class HeadPresetStacks {
	private HeadPresetStacks() {
	}

	public static ItemStack create(HeadPresetEntry entry) {
		if (entry == null || entry.base64() == null || entry.base64().isBlank()) {
			return new ItemStack(Items.PLAYER_HEAD);
		}
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(entry.name()).withStyle(ChatFormatting.RESET));
		HeadProfileHelper.fromTextureBase64(entry.name(), entry.base64())
			.ifPresent(profile -> stack.set(DataComponents.PROFILE, profile));
		return stack;
	}
}
