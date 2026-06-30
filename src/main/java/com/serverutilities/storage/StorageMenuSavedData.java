package com.serverutilities.storage;

import com.serverutilities.ServerUtilitiesMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StorageMenuSavedData extends SavedData {
	private record StoredMenu(BlockPos pos, MenuEntry entry) {
		private static final Codec<StoredMenu> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(StoredMenu::pos),
			MenuEntry.CODEC.fieldOf("menu").forGetter(StoredMenu::entry)
		).apply(instance, StoredMenu::new));
	}

	public static final Codec<StorageMenuSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		StoredMenu.CODEC.listOf().fieldOf("menus").forGetter(StorageMenuSavedData::toStoredMenus)
	).apply(instance, StorageMenuSavedData::fromStoredMenus));

	public static final SavedDataType<StorageMenuSavedData> TYPE = new SavedDataType<>(
		ServerUtilitiesMod.id("storage_menus"),
		StorageMenuSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<BlockPos, MenuEntry> menus;

	public StorageMenuSavedData() {
		this(new HashMap<>());
	}

	public StorageMenuSavedData(Map<BlockPos, MenuEntry> menus) {
		this.menus = new HashMap<>(menus);
	}

	private static StorageMenuSavedData fromStoredMenus(List<StoredMenu> storedMenus) {
		Map<BlockPos, MenuEntry> map = new HashMap<>();
		for (StoredMenu storedMenu : storedMenus) {
			map.put(storedMenu.pos().immutable(), storedMenu.entry());
		}
		return new StorageMenuSavedData(map);
	}

	private List<StoredMenu> toStoredMenus() {
		return menus.entrySet().stream()
			.map(entry -> new StoredMenu(entry.getKey().immutable(), entry.getValue()))
			.toList();
	}

	public Optional<StorageMenuDefinition> get(BlockPos pos) {
		MenuEntry entry = menus.get(pos);
		return entry == null ? Optional.empty() : Optional.of(entry.toDefinition());
	}

	public void put(BlockPos pos, StorageMenuDefinition definition) {
		if (!definition.enabled() && !definition.hasConfiguredSlots()) {
			menus.remove(pos);
		} else {
			menus.put(pos, MenuEntry.fromDefinition(definition));
		}
		setDirty();
	}

	public void remove(BlockPos pos) {
		if (menus.remove(pos) != null) {
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
			String resolvedSubMenuId = subMenuId;
			if (slotType == StorageMenuSlotType.LINK && resolvedSubMenuId.isBlank()) {
				resolvedSubMenuId = StorageSubMenuManager.createId();
			}
			return new StorageMenuSlotConfig(index, slotType, item, command, resolvedSubMenuId);
		}
	}
}
