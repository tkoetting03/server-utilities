package com.serverutilities.client.mixin;

import com.serverutilities.client.screen.EditorMousePreservation;
import com.serverutilities.client.screen.ModEditorGuiState;
import com.serverutilities.client.storage.StorageMenuClientSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void serverutilities$armMousePreservation(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (StorageMenuClientSlots.shouldPreserveMouse(screen.getMenu(), slot, containerInput)) {
			EditorMousePreservation.arm();
		}
	}

	@Redirect(
		method = "keyPressed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/KeyMapping;matches(Lnet/minecraft/client/input/KeyEvent;)Z"
		)
	)
	private boolean serverutilities$allowTypingInventoryKey(KeyMapping mapping, KeyEvent event) {
		boolean matches = mapping.matches(event);
		if (!matches) {
			return false;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		if (mapping == Minecraft.getInstance().options.keyInventory
			&& ModEditorGuiState.shouldSuppressInventoryClose(screen)) {
			return false;
		}
		return true;
	}
}
