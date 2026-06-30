package com.serverutilities.storage;

import com.serverutilities.text.TextFormats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class StorageMenuItemLore {
	public static final int MAX_LINES = 12;

	private StorageMenuItemLore() {
	}

	public static List<String> readLoreLines(ItemStack stack) {
		if (stack.isEmpty()) {
			return List.of();
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null || lore.lines().isEmpty()) {
			return List.of();
		}
		List<String> lines = new ArrayList<>(lore.lines().size());
		for (Component line : lore.lines()) {
			String serialized = TextFormats.fromComponent(line);
			if (!serialized.isBlank()) {
				lines.add(serialized);
			}
		}
		return lines;
	}

	public static ItemStack withLore(ItemStack stack, List<String> serializedLines) {
		ItemStack copy = stack.copy();
		List<Component> components = new ArrayList<>();
		for (String serialized : serializedLines) {
			String normalized = TextFormats.normalize(serialized);
			if (normalized.isBlank()) {
				continue;
			}
			Component line = TextFormats.toComponent(normalized);
			if (line.getStyle().isEmpty()) {
				line = line.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
			}
			components.add(line);
		}
		if (components.isEmpty()) {
			copy.remove(DataComponents.LORE);
		} else {
			copy.set(DataComponents.LORE, new ItemLore(components));
		}
		return copy;
	}
}
