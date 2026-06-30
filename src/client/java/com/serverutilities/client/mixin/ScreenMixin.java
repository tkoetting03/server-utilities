package com.serverutilities.client.mixin;

import com.serverutilities.client.screen.DraggablePanelPositions;
import com.serverutilities.client.screen.ModUiRenderContext;
import com.serverutilities.client.screen.ModUiSelectionState;
import com.serverutilities.client.screen.StorageMenuEditorOverlay;
import com.serverutilities.client.screen.StorageMenuNameOverlay;
import com.serverutilities.client.screen.ItemStylerOverlay;
import com.serverutilities.client.screen.ParticlePresetPickerOverlay;
import com.serverutilities.client.screen.NpcHologramStackOverlay;
import com.serverutilities.client.screen.TextStyleOverlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void serverutilities$closeStyleOverlay(CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		DraggablePanelPositions.clearScreen(screen);
		ModUiRenderContext.clearScreen(screen);
		ModUiSelectionState.clearScreen(screen);
		TextStyleOverlay.onScreenRemoved(screen);
		NpcHologramStackOverlay.onScreenRemoved(screen);
		ParticlePresetPickerOverlay.onScreenRemoved(screen);
		ItemStylerOverlay.onScreenRemoved(screen);
		StorageMenuNameOverlay.onScreenRemoved(screen);
		StorageMenuEditorOverlay.onScreenRemoved(screen);
	}
}
