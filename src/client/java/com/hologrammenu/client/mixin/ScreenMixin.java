package com.hologrammenu.client.mixin;

import com.hologrammenu.client.screen.DraggablePanelPositions;
import com.hologrammenu.client.screen.ModUiRenderContext;
import com.hologrammenu.client.screen.ModUiSelectionState;
import com.hologrammenu.client.screen.StorageMenuEditorOverlay;
import com.hologrammenu.client.screen.StorageMenuNameOverlay;
import com.hologrammenu.client.screen.HeadPresetPickerOverlay;
import com.hologrammenu.client.screen.ItemStylerOverlay;
import com.hologrammenu.client.screen.ParticlePresetPickerOverlay;
import com.hologrammenu.client.screen.NpcHologramStackOverlay;
import com.hologrammenu.client.screen.TextStyleOverlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void hologrammenu$closeStyleOverlay(CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		DraggablePanelPositions.clearScreen(screen);
		ModUiRenderContext.clearScreen(screen);
		ModUiSelectionState.clearScreen(screen);
		TextStyleOverlay.onScreenRemoved(screen);
		NpcHologramStackOverlay.onScreenRemoved(screen);
		HeadPresetPickerOverlay.onScreenRemoved(screen);
		ParticlePresetPickerOverlay.onScreenRemoved(screen);
		ItemStylerOverlay.onScreenRemoved(screen);
		StorageMenuNameOverlay.onScreenRemoved(screen);
		StorageMenuEditorOverlay.onScreenRemoved(screen);
	}
}
