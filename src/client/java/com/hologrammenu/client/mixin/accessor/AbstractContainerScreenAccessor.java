package com.hologrammenu.client.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int hologrammenu$getLeftPos();

	@Accessor("topPos")
	int hologrammenu$getTopPos();

	@Accessor("imageWidth")
	int hologrammenu$getImageWidth();

	@Accessor("imageHeight")
	int hologrammenu$getImageHeight();
}
