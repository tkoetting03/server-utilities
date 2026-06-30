package com.hologrammenu.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageMenuFillerItems {
	private StorageMenuFillerItems() {
	}

	public static ItemStack defaultFiller() {
		ItemStack stack = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		return stack;
	}

	public static ItemStack backButton() {
		ItemStack stack = new ItemStack(Items.ARROW);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Back").withStyle(ChatFormatting.YELLOW));
		return stack;
	}
}
