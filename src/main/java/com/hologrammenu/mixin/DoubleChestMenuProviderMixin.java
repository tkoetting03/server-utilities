package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuOpener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.ChestBlock$2$1")
public abstract class DoubleChestMenuProviderMixin {
	@Shadow
	@Final
	private ChestBlockEntity val$first;

	@Shadow
	@Final
	private Container val$container;

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$openDoubleChestStorageMenu(int syncId, Inventory playerInventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
		if (!(val$first.getLevel() instanceof ServerLevel level)) {
			return;
		}

		AbstractContainerMenu menu = StorageMenuOpener.tryOpen(
			level,
			val$first.getBlockPos(),
			val$container.getContainerSize(),
			syncId,
			playerInventory,
			player,
			val$container,
			val$first
		);
		if (menu != null) {
			cir.setReturnValue(menu);
		}
	}
}
