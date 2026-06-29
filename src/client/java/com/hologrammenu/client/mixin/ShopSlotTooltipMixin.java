package com.hologrammenu.client.mixin;

import com.hologrammenu.client.storage.ShopClientState;
import com.hologrammenu.storage.ShopDescriptions;
import com.hologrammenu.storage.ShopListing;
import com.hologrammenu.storage.StorageMenuLocator;
import com.hologrammenu.storage.StorageMenuSlotType;
import com.hologrammenu.storage.VirtualStorageContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
public abstract class ShopSlotTooltipMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
	private void hologrammenu$appendShopPriceTooltip(
		ItemStack stack,
		CallbackInfoReturnable<List<Component>> cir
	) {
		if (hoveredSlot == null || !hoveredSlot.hasItem() || !ItemStack.isSameItemSameComponents(stack, hoveredSlot.getItem())) {
			return;
		}

		VirtualStorageContainer container = VirtualStorageContainer.asVirtual(
			StorageMenuLocator.extractContainer(((AbstractContainerScreen<?>) (Object) this).getMenu())
		);
		if (container == null || !container.viewContext().isRoot()) {
			return;
		}

		int menuSlot = hoveredSlot.getContainerSlot();
		if (container.definition().slot(menuSlot).type() != StorageMenuSlotType.SHOP_ITEM) {
			return;
		}

		BlockPos pos = container.blockPos();
		Optional<ShopListing> listing = ShopClientState.get(pos).listing(menuSlot);
		if (listing.isEmpty() || !listing.get().isConfigured()) {
			return;
		}

		Component price = ShopDescriptions.priceTooltip(listing.get());
		if (price.getString().isEmpty()) {
			return;
		}

		List<Component> tooltip = new ArrayList<>(cir.getReturnValue());
		tooltip.add(price);
		cir.setReturnValue(tooltip);
	}
}
