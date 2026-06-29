package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuInteractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class StorageMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void hologrammenu$handleStorageMenuClick(int slotIndex, int button, ContainerInput input, Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		if (StorageMenuInteractions.handleClick((AbstractContainerMenu) (Object) this, slotIndex, input, serverPlayer)) {
			ci.cancel();
		}
	}
}
