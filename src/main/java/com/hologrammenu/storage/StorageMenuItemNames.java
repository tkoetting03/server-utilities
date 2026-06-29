package com.hologrammenu.storage;

import com.hologrammenu.text.TextFormats;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class StorageMenuItemNames {
	private StorageMenuItemNames() {
	}

	public static String readCustomName(ItemStack stack) {
		if (stack.isEmpty()) {
			return "";
		}
		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName == null) {
			return "";
		}
		if (customName.getString().isBlank()) {
			return "";
		}
		return TextFormats.fromComponent(customName);
	}

	public static ItemStack withCustomName(ItemStack stack, String serializedName) {
		ItemStack copy = stack.copy();
		String normalized = TextFormats.normalize(serializedName);
		if (normalized.isBlank()) {
			copy.remove(DataComponents.CUSTOM_NAME);
			return copy;
		}
		copy.set(DataComponents.CUSTOM_NAME, TextFormats.toComponent(normalized));
		return copy;
	}
}
