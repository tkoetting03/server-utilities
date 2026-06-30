package com.serverutilities.mixin;

import com.serverutilities.storage.StorageMenuOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMenuMixin {
	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void serverutilities$openStorageMenu(int syncId, Inventory playerInventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (!(self.getLevel() instanceof ServerLevel level) || !(self instanceof BaseContainerBlockEntity containerBlockEntity)) {
			return;
		}

		BlockPos pos = self.getBlockPos();
		int containerSize = StorageMenuOpener.resolveContainerSize(level, pos);
		if (containerSize <= 0) {
			return;
		}

		AbstractContainerMenu menu = StorageMenuOpener.tryOpen(
			level,
			pos,
			containerSize,
			syncId,
			playerInventory,
			player,
			containerBlockEntity,
			self
		);
		if (menu != null) {
			cir.setReturnValue(menu);
		}
	}
}
