package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuChrome;
import com.hologrammenu.storage.VirtualStorageContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class StorageMenuCloseSlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$blockChromeSlotPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (isChromeSlot((Slot) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$blockChromeSlotPickup(CallbackInfoReturnable<Boolean> cir) {
		if (isChromeSlot((Slot) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	private static boolean isChromeSlot(Slot slot) {
		VirtualStorageContainer container = VirtualStorageContainer.asVirtual(slot.container);
		return container != null && StorageMenuChrome.isChromeSlot(container, slot.getContainerSlot());
	}
}
