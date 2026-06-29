package com.hologrammenu.storage;

import com.hologrammenu.network.StorageMenuNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.Optional;

public final class StorageMenuOpener {
	private StorageMenuOpener() {
	}

	public static AbstractContainerMenu tryOpen(
		ServerLevel level,
		BlockPos openPos,
		int containerSize,
		int syncId,
		Inventory playerInventory,
		Player player,
		Container validitySource,
		BlockEntity typeSource
	) {
		ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
		if (serverPlayer != null) {
			StorageMenuNavigation.onDirectOpen(serverPlayer);
		}

		StorageMenuViewContext viewContext = StorageMenuViewContext.root(openPos);
		Optional<StorageMenuManager.ResolvedMenu> resolved = StorageMenuManager.resolveActive(level, openPos);

		return resolved
			.map(menu -> buildMenu(
				level,
				viewContext,
				menu.definition(),
				syncId,
				playerInventory,
				validitySource,
				typeSource,
				serverPlayer
			))
			.orElse(null);
	}

	public static void openBlock(ServerPlayer player, BlockPos pos) {
		ServerLevel level = (ServerLevel) player.level();
		Optional<StorageMenuBlockData> data = StorageMenuBlockStore.get(level, pos);
		if (data.isEmpty() || !data.get().definition().enabled()) {
			return;
		}

		StorageMenuDefinition definition = data.get().definition();
		StorageMenuViewContext viewContext = StorageMenuViewContext.root(pos);
		OpenContext openContext = resolveOpenContext(level, pos);
		StorageMenuNavigation.onDirectOpen(player);
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return definition.displayTitle().orElse(Component.translatable("screen.hologrammenu.storage_menu.title"));
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
				return buildMenu(
					level,
					viewContext,
					definition,
					syncId,
					playerInventory,
					openContext.validitySource(),
					openContext.typeSource(),
					player
				);
			}
		});
	}

	public static void openView(ServerPlayer player, StorageMenuViewContext viewContext, int containerSize) {
		ServerLevel level = (ServerLevel) player.level();
		OpenContext openContext = resolveOpenContext(level, viewContext.anchorPos());
		StorageMenuDefinition definition = resolveDefinition(level, viewContext, containerSize);
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				if (openContext.typeSource() instanceof BaseContainerBlockEntity containerBlockEntity) {
					return definition.displayTitle().orElse(containerBlockEntity.getDisplayName());
				}
				return definition.displayTitle().orElse(Component.translatable("screen.hologrammenu.storage_menu.title"));
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
				return buildMenu(
					level,
					viewContext,
					definition,
					syncId,
					playerInventory,
					openContext.validitySource(),
					openContext.typeSource(),
					player
				);
			}
		});

		StorageMenuNavigation.setCurrentView(player, viewContext);
	}

	private record OpenContext(Container validitySource, BlockEntity typeSource) {
	}

	private static OpenContext resolveOpenContext(ServerLevel level, BlockPos pos) {
		BlockEntity anchor = level.getBlockEntity(pos);
		if (anchor instanceof BaseContainerBlockEntity containerBlockEntity) {
			Container validitySource = resolveValiditySource(level, pos, containerBlockEntity);
			if (validitySource != null) {
				return new OpenContext(validitySource, anchor);
			}
		}
		return new OpenContext(new StorageMenuBlockValidityContainer(level, pos), anchor);
	}

	private static StorageMenuDefinition resolveDefinition(ServerLevel level, StorageMenuViewContext viewContext, int containerSize) {
		if (viewContext.isRoot()) {
			return StorageMenuManager.resolveActive(level, viewContext.anchorPos())
				.map(StorageMenuManager.ResolvedMenu::definition)
				.orElse(StorageMenuDefinition.empty(containerSize));
		}
		return StorageSubMenuManager.get(level, viewContext.subMenuId())
			.orElse(StorageMenuDefinition.empty(containerSize).withEnabled(true));
	}

	private static AbstractContainerMenu buildMenu(
		ServerLevel level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		int syncId,
		Inventory playerInventory,
		Container validitySource,
		BlockEntity typeSource,
		ServerPlayer serverPlayer
	) {
		ShopDefinition shop = ShopDefinition.EMPTY;
		if (viewContext.isRoot()) {
			shop = StorageMenuBlockStore.get(level, viewContext.anchorPos())
				.map(StorageMenuBlockData::shop)
				.orElse(ShopDefinition.EMPTY);
		}
		if (serverPlayer != null) {
			definition = StorageMenuChrome.applyRuntimeChrome(definition, serverPlayer);
			StorageMenuNetworkHandler.sendContext(serverPlayer, viewContext);
			StorageMenuNetworkHandler.sendNavigationState(serverPlayer);
			StorageMenuNetworkHandler.sendShopState(serverPlayer, viewContext.anchorPos(), shop);
			StorageMenuNavigation.setCurrentView(serverPlayer, viewContext);
		}
		return StorageMenuFactory.create(level, viewContext, definition, shop, syncId, playerInventory, validitySource, typeSource);
	}

	public static int resolveContainerSize(ServerLevel level, BlockPos pos) {
		return StorageMenuBlockStore.get(level, pos)
			.filter(data -> data.definition().enabled())
			.map(data -> data.definition().containerSize())
			.orElseGet(() -> resolvePhysicalContainerSize(level, pos));
	}

	public static int resolvePhysicalContainerSize(ServerLevel level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof BaseContainerBlockEntity containerBlockEntity)) {
			return 0;
		}

		if (blockEntity instanceof ChestBlockEntity && blockEntity.getBlockState().getBlock() instanceof ChestBlock chestBlock) {
			Container container = ChestBlock.getContainer(chestBlock, blockEntity.getBlockState(), level, pos, false);
			if (container != null) {
				return container.getContainerSize();
			}
		}

		return containerBlockEntity.getContainerSize();
	}

	private static Container resolveValiditySource(ServerLevel level, BlockPos pos, BaseContainerBlockEntity blockEntity) {
		if (blockEntity instanceof ChestBlockEntity && blockEntity.getBlockState().getBlock() instanceof ChestBlock chestBlock) {
			Container container = ChestBlock.getContainer(chestBlock, blockEntity.getBlockState(), level, pos, false);
			if (container != null) {
				return container;
			}
		}
		return blockEntity;
	}
}
