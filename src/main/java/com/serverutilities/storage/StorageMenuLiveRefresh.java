package com.serverutilities.storage;

import com.serverutilities.network.StorageMenuNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class StorageMenuLiveRefresh {
	private StorageMenuLiveRefresh() {
	}

	public static void refreshViewers(ServerLevel level, BlockPos pos, StorageMenuDefinition definition) {
		ShopDefinition shop = StorageMenuBlockStore.get(level, pos).map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY);
		for (ServerPlayer player : level.players()) {
			AbstractContainerMenu menu = player.containerMenu;
			if (menu == null) {
				continue;
			}
			VirtualStorageContainer container = findVirtualContainer(menu);
			if (container == null) {
				continue;
			}
			if (!container.viewContext().anchorPos().equals(pos)) {
				continue;
			}
			if (container.viewContext().isRoot()) {
				StorageMenuDefinition updated = StorageMenuChrome.applyRuntimeChrome(definition, player);
				container.setDefinition(updated);
			} else {
				StorageMenuDefinition subDef = StorageSubMenuManager.get(level, container.subMenuId()).orElse(container.definition());
				StorageMenuDefinition updated = StorageMenuChrome.applyRuntimeChrome(subDef, player);
				container.setDefinition(updated);
			}
			container.setShop(shop);
			menu.slotsChanged(container);
			menu.broadcastFullState();
			if (player instanceof ServerPlayer serverPlayer) {
				StorageMenuNetworkHandler.sendShopState(serverPlayer, pos, shop);
			}
		}
	}

	private static VirtualStorageContainer findVirtualContainer(AbstractContainerMenu menu) {
		for (var slot : menu.slots) {
			if (slot.container instanceof VirtualStorageContainer virtual) {
				return virtual;
			}
		}
		return null;
	}
}
