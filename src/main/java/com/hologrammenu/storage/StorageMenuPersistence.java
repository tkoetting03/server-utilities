package com.hologrammenu.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class StorageMenuPersistence {
	private StorageMenuPersistence() {
	}

	public record MenuEntry(boolean enabled, int containerSize, String title, List<SlotEntry> slots) {
		public static final Codec<MenuEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("enabled").forGetter(MenuEntry::enabled),
			Codec.INT.fieldOf("container_size").forGetter(MenuEntry::containerSize),
			Codec.STRING.lenientOptionalFieldOf("title").forGetter(entry -> entry.title().isBlank() ? Optional.empty() : Optional.of(entry.title())),
			SlotEntry.CODEC.listOf().fieldOf("slots").forGetter(MenuEntry::slots)
		).apply(instance, (enabled, containerSize, title, slots) -> new MenuEntry(enabled, containerSize, title.orElse(""), slots)));

		public static MenuEntry fromDefinition(StorageMenuDefinition definition) {
			List<SlotEntry> slots = definition.asList().stream()
				.filter(config -> config.type().isEditable())
				.map(SlotEntry::fromConfig)
				.toList();
			return new MenuEntry(definition.enabled(), definition.containerSize(), definition.title(), slots);
		}

		public StorageMenuDefinition toDefinition() {
			StorageMenuDefinition definition = StorageMenuDefinition.empty(containerSize)
				.withEnabled(enabled)
				.withTitle(title == null ? "" : title);
			for (SlotEntry slot : slots) {
				StorageMenuSlotConfig config = slot.toConfig();
				if (config.type().isEditable()) {
					definition = definition.withSlot(config);
				}
			}
			return definition;
		}
	}

	public record SlotEntry(int index, String type, ItemStack item, String command, String subMenuId) {
		public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("index").forGetter(SlotEntry::index),
			Codec.STRING.fieldOf("type").forGetter(SlotEntry::type),
			ItemStack.CODEC.fieldOf("item").forGetter(SlotEntry::item),
			Codec.STRING.lenientOptionalFieldOf("command").forGetter(entry -> entry.command().isBlank() ? Optional.empty() : Optional.of(entry.command())),
			Codec.STRING.lenientOptionalFieldOf("sub_menu_id").forGetter(entry -> entry.subMenuId().isBlank() ? Optional.empty() : Optional.of(entry.subMenuId()))
		).apply(instance, (index, type, item, command, subMenuId) -> new SlotEntry(
			index,
			type,
			item,
			command.orElse(""),
			subMenuId.orElse(StorageMenuSlotConfig.NO_SUB_MENU)
		)));

		public static SlotEntry fromConfig(StorageMenuSlotConfig config) {
			return new SlotEntry(
				config.index(),
				config.type().name(),
				config.displayStack(),
				config.command(),
				config.subMenuId() == null ? StorageMenuSlotConfig.NO_SUB_MENU : config.subMenuId()
			);
		}

		public StorageMenuSlotConfig toConfig() {
			StorageMenuSlotType slotType;
			try {
				slotType = StorageMenuSlotType.valueOf(type);
			} catch (IllegalArgumentException ignored) {
				slotType = StorageMenuSlotType.EMPTY;
			}
			String resolvedSubMenuId = subMenuId;
			if (slotType == StorageMenuSlotType.LINK && resolvedSubMenuId.isBlank()) {
				resolvedSubMenuId = StorageSubMenuManager.createId();
			}
			return new StorageMenuSlotConfig(index, slotType, item, command, resolvedSubMenuId);
		}
	}
}
