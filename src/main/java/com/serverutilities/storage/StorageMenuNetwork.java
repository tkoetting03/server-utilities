package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StorageMenuNetwork {
	public record SlotData(int index, int type, ItemStack stack, String command, String subMenuId) {
		public static final StreamCodec<RegistryFriendlyByteBuf, SlotData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SlotData::index,
			ByteBufCodecs.VAR_INT,
			SlotData::type,
			ItemStack.OPTIONAL_STREAM_CODEC,
			SlotData::stack,
			ByteBufCodecs.STRING_UTF8,
			SlotData::command,
			ByteBufCodecs.STRING_UTF8,
			SlotData::subMenuId,
			SlotData::new
		);

		public static SlotData fromConfig(StorageMenuSlotConfig config) {
			return new SlotData(
				config.index(),
				config.type().ordinal(),
				config.displayStack(),
				config.command(),
				config.subMenuId() == null ? StorageMenuSlotConfig.NO_SUB_MENU : config.subMenuId()
			);
		}

		public StorageMenuSlotConfig toConfig() {
			return new StorageMenuSlotConfig(
				index,
				StorageMenuSlotType.fromId(type),
				stack == null ? ItemStack.EMPTY : stack,
				command,
				subMenuId == null ? StorageMenuSlotConfig.NO_SUB_MENU : subMenuId
			);
		}
	}

	public record ShopListingData(int slotIndex, ItemStack product, ItemStack cost, int stock) {
		public static final StreamCodec<RegistryFriendlyByteBuf, ShopListingData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ShopListingData::slotIndex,
			ItemStack.OPTIONAL_STREAM_CODEC,
			ShopListingData::product,
			ItemStack.OPTIONAL_STREAM_CODEC,
			ShopListingData::cost,
			ByteBufCodecs.VAR_INT,
			ShopListingData::stock,
			ShopListingData::new
		);

		public static ShopListingData fromListing(ShopListing listing) {
			return new ShopListingData(
				listing.slotIndex(),
				listing.product(),
				listing.cost(),
				listing.stock()
			);
		}

		public ShopListing toListing() {
			return new ShopListing(slotIndex, product, cost, stock);
		}
	}

	public record MenuData(
		BlockPos pos,
		String subMenuId,
		int containerSize,
		boolean enabled,
		String title,
		List<SlotData> slots,
		boolean invulnerable,
		boolean hologramLabel,
		StorageMenuHologramSettings hologramSettings,
		boolean shopEnabled,
		List<ShopListingData> shopListings,
		int npcEntityId
	) {
		public static final StreamCodec<RegistryFriendlyByteBuf, MenuData> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			MenuData::pos,
			ByteBufCodecs.STRING_UTF8,
			MenuData::subMenuId,
			ByteBufCodecs.VAR_INT,
			MenuData::containerSize,
			ByteBufCodecs.BOOL,
			MenuData::enabled,
			ByteBufCodecs.STRING_UTF8,
			MenuData::title,
			SlotData.STREAM_CODEC.apply(ByteBufCodecs.list()),
			MenuData::slots,
			ByteBufCodecs.BOOL,
			MenuData::invulnerable,
			ByteBufCodecs.BOOL,
			MenuData::hologramLabel,
			StorageMenuHologramSettings.STREAM_CODEC,
			MenuData::hologramSettings,
			ByteBufCodecs.BOOL,
			MenuData::shopEnabled,
			ShopListingData.STREAM_CODEC.apply(ByteBufCodecs.list()),
			MenuData::shopListings,
			ByteBufCodecs.VAR_INT,
			MenuData::npcEntityId,
			MenuData::new
		);

		public static MenuData fromDefinition(
			StorageMenuViewContext viewContext,
			StorageMenuDefinition definition,
			boolean invulnerable,
			boolean hologramLabel,
			StorageMenuHologramSettings hologramSettings,
			ShopDefinition shop
		) {
			List<SlotData> slots = definition.asList().stream()
				.filter(config -> config.type().isEditable())
				.map(SlotData::fromConfig)
				.toList();
			List<ShopListingData> listings = viewContext.isRoot() && shop != null
				? shop.asList().stream().map(ShopListingData::fromListing).toList()
				: List.of();
			boolean shopEnabled = viewContext.isRoot() && shop != null && shop.enabled();
			return new MenuData(
				viewContext.anchorPos(),
				viewContext.subMenuId() == null ? StorageMenuViewContext.NO_SUB_MENU : viewContext.subMenuId(),
				definition.containerSize(),
				definition.enabled(),
				definition.title(),
				slots,
				invulnerable,
				hologramLabel,
				hologramSettings == null ? StorageMenuHologramSettings.DEFAULT : hologramSettings,
				shopEnabled,
				listings,
				viewContext.npcEntityId()
			);
		}

		public static MenuData fromDefinition(
			StorageMenuViewContext viewContext,
			StorageMenuDefinition definition,
			boolean invulnerable,
			boolean hologramLabel,
			ShopDefinition shop
		) {
			return fromDefinition(viewContext, definition, invulnerable, hologramLabel, StorageMenuHologramSettings.DEFAULT, shop);
		}

		public static MenuData fromDefinition(StorageMenuViewContext viewContext, StorageMenuDefinition definition, boolean invulnerable, boolean hologramLabel) {
			return fromDefinition(viewContext, definition, invulnerable, hologramLabel, ShopDefinition.EMPTY);
		}

		public static MenuData fromDefinition(StorageMenuViewContext viewContext, StorageMenuDefinition definition, boolean invulnerable) {
			return fromDefinition(viewContext, definition, invulnerable, false, ShopDefinition.EMPTY);
		}

		public StorageMenuViewContext viewContext() {
			if (npcEntityId >= 0) {
				if (subMenuId == null || subMenuId.isBlank()) {
					return StorageMenuViewContext.forNpc(npcEntityId);
				}
				return StorageMenuViewContext.forNpc(npcEntityId, subMenuId);
			}
			StorageMenuViewContext root = StorageMenuViewContext.root(pos);
			if (subMenuId == null || subMenuId.isBlank()) {
				return root;
			}
			return root.withSubMenu(subMenuId);
		}

		public StorageMenuDefinition toDefinition() {
			StorageMenuDefinition definition = StorageMenuDefinition.empty(containerSize)
				.withEnabled(enabled)
				.withTitle(title);
			List<StorageMenuSlotConfig> configs = new ArrayList<>();
			for (SlotData slot : slots) {
				StorageMenuSlotConfig config = slot.toConfig();
				if (config.type().isEditable()) {
					configs.add(config);
				}
			}
			return definition.withSlots(configs);
		}

		public ShopDefinition toShopDefinition() {
			java.util.Map<Integer, ShopListing> map = new java.util.HashMap<>();
			for (ShopListingData listing : shopListings) {
				ShopListing resolved = listing.toListing();
				if (resolved.isConfigured()) {
					map.put(resolved.slotIndex(), resolved);
				}
			}
			return new ShopDefinition(shopEnabled, map);
		}
	}

	private StorageMenuNetwork() {
	}
}
