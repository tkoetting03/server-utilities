package com.serverutilities.client.storage;

import com.serverutilities.storage.ShopListing;
import com.serverutilities.storage.StorageMenuLocator;
import com.serverutilities.storage.StorageMenuSlotConfig;
import com.serverutilities.storage.StorageMenuSlotType;
import com.serverutilities.storage.VirtualStorageContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public final class StorageMenuClientSlots {
	private StorageMenuClientSlots() {
	}

	public static boolean shouldPreserveMouse(AbstractContainerMenu menu, Slot slot, ContainerInput input) {
		if (slot == null || input != ContainerInput.PICKUP) {
			return false;
		}

		VirtualStorageContainer container = VirtualStorageContainer.asVirtual(StorageMenuLocator.extractContainer(menu));
		if (container == null || slot.container != container) {
			return false;
		}

		int menuSlot = slot.getContainerSlot();
		StorageMenuSlotConfig config = container.definition().slot(menuSlot);
		if (container.viewContext().isRoot() && container.shop().enabled() && config.type() == StorageMenuSlotType.SHOP_ITEM) {
			return container.shop().listing(menuSlot).filter(ShopListing::isConfigured).isPresent();
		}

		return config.blocksInteraction();
	}
}
