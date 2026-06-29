package com.hologrammenu.storage;

import com.hologrammenu.HologramMenuMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StorageSubMenuSavedData extends SavedData {
	private record StoredSubMenu(String id, MenuEntry entry) {
		private static final Codec<StoredSubMenu> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(StoredSubMenu::id),
			MenuEntry.CODEC.fieldOf("menu").forGetter(StoredSubMenu::entry)
		).apply(instance, StoredSubMenu::new));
	}

	public static final Codec<StorageSubMenuSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		StoredSubMenu.CODEC.listOf().fieldOf("sub_menus").forGetter(StorageSubMenuSavedData::toStoredSubMenus)
	).apply(instance, StorageSubMenuSavedData::fromStoredSubMenus));

	public static final SavedDataType<StorageSubMenuSavedData> TYPE = new SavedDataType<>(
		HologramMenuMod.id("storage_sub_menus"),
		StorageSubMenuSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, MenuEntry> subMenus;

	public StorageSubMenuSavedData() {
		this(new HashMap<>());
	}

	public StorageSubMenuSavedData(Map<String, MenuEntry> subMenus) {
		this.subMenus = new HashMap<>(subMenus);
	}

	private static StorageSubMenuSavedData fromStoredSubMenus(List<StoredSubMenu> stored) {
		Map<String, MenuEntry> map = new HashMap<>();
		for (StoredSubMenu entry : stored) {
			map.put(entry.id(), entry.entry());
		}
		return new StorageSubMenuSavedData(map);
	}

	private List<StoredSubMenu> toStoredSubMenus() {
		return subMenus.entrySet().stream()
			.map(entry -> new StoredSubMenu(entry.getKey(), entry.getValue()))
			.toList();
	}

	public Optional<StorageMenuDefinition> get(String id) {
		MenuEntry entry = subMenus.get(id);
		return entry == null ? Optional.empty() : Optional.of(entry.toDefinition());
	}

	public void put(String id, StorageMenuDefinition definition) {
		if (!definition.enabled() && !definition.hasConfiguredSlots()) {
			subMenus.remove(id);
		} else {
			subMenus.put(id, MenuEntry.fromDefinition(definition));
		}
		setDirty();
	}

	public void remove(String id) {
		if (subMenus.remove(id) != null) {
			setDirty();
		}
	}

	private record MenuEntry(boolean enabled, int containerSize, String title, List<SlotEntry> slots) {
		private static final Codec<MenuEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("enabled").forGetter(MenuEntry::enabled),
			Codec.INT.fieldOf("container_size").forGetter(MenuEntry::containerSize),
			Codec.STRING.lenientOptionalFieldOf("title").forGetter(entry -> entry.title().isBlank() ? Optional.empty() : Optional.of(entry.title())),
			SlotEntry.CODEC.listOf().fieldOf("slots").forGetter(MenuEntry::slots)
		).apply(instance, (enabled, containerSize, title, slots) -> new MenuEntry(enabled, containerSize, title.orElse(""), slots)));

		private static MenuEntry fromDefinition(StorageMenuDefinition definition) {
			List<SlotEntry> slots = definition.asList().stream()
				.filter(config -> config.type().isEditable())
				.map(SlotEntry::fromConfig)
				.toList();
			return new MenuEntry(definition.enabled(), definition.containerSize(), definition.title(), slots);
		}

		private StorageMenuDefinition toDefinition() {
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

	private record SlotEntry(int index, String type, ItemStack item, String command, String subMenuId) {
		private static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
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

		private static SlotEntry fromConfig(StorageMenuSlotConfig config) {
			return new SlotEntry(
				config.index(),
				config.type().name(),
				config.displayStack(),
				config.command(),
				config.subMenuId() == null ? StorageMenuSlotConfig.NO_SUB_MENU : config.subMenuId()
			);
		}

		private StorageMenuSlotConfig toConfig() {
			StorageMenuSlotType slotType;
			try {
				slotType = StorageMenuSlotType.valueOf(type);
			} catch (IllegalArgumentException ignored) {
				slotType = StorageMenuSlotType.EMPTY;
			}
			return new StorageMenuSlotConfig(index, slotType, item, command, subMenuId);
		}
	}
}
