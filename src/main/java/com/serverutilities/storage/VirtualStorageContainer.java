package com.serverutilities.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class VirtualStorageContainer implements Container {
	private final Level level;
	private final StorageMenuViewContext viewContext;
	private StorageMenuDefinition definition;
	private ShopDefinition shop = ShopDefinition.EMPTY;
	private final Container validitySource;

	public VirtualStorageContainer(
		Level level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		Container validitySource
	) {
		this(level, viewContext, definition, ShopDefinition.EMPTY, validitySource);
	}

	public VirtualStorageContainer(
		Level level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		ShopDefinition shop,
		Container validitySource
	) {
		this.level = level;
		this.viewContext = viewContext.immutable();
		this.definition = definition;
		this.shop = shop == null ? ShopDefinition.EMPTY : shop;
		this.validitySource = validitySource;
	}

	public StorageMenuViewContext viewContext() {
		return viewContext;
	}

	public BlockPos blockPos() {
		return viewContext.anchorPos();
	}

	public String subMenuId() {
		return viewContext.subMenuId();
	}

	public boolean isSubMenu() {
		return !viewContext.isRoot();
	}

	public StorageMenuDefinition definition() {
		return definition;
	}

	public void setDefinition(StorageMenuDefinition newDefinition) {
		this.definition = newDefinition;
	}

	public ShopDefinition shop() {
		return shop;
	}

	public void setShop(ShopDefinition newShop) {
		this.shop = newShop == null ? ShopDefinition.EMPTY : newShop;
	}

	@Override
	public int getContainerSize() {
		return definition.containerSize();
	}

	@Override
	public boolean isEmpty() {
		for (int index = 0; index < getContainerSize(); index++) {
			if (!getItem(index).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (slot < 0 || slot >= getContainerSize()) {
			return ItemStack.EMPTY;
		}
		if (viewContext.isRoot() && shop.enabled()) {
			StorageMenuSlotConfig config = definition.slot(slot);
			if (config.type() == StorageMenuSlotType.SHOP_ITEM) {
				return shop.listing(slot)
					.filter(ShopListing::isConfigured)
					.map(ShopListing::product)
					.orElse(ItemStack.EMPTY);
			}
		}
		return definition.displayStack(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return validitySource.stillValid(player);
	}

	@Override
	public void startOpen(ContainerUser user) {
		validitySource.startOpen(user);
	}

	@Override
	public void stopOpen(ContainerUser user) {
		validitySource.stopOpen(user);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public void clearContent() {
	}

	public static boolean isVirtual(Container container) {
		return container instanceof VirtualStorageContainer;
	}

	public static VirtualStorageContainer asVirtual(Container container) {
		return container instanceof VirtualStorageContainer virtual ? virtual : null;
	}

	public BlockEntity blockEntity() {
		return level.isClientSide() ? null : level.getBlockEntity(viewContext.anchorPos());
	}
}
