package com.hologrammenu.client.mixin.accessor;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {
	@Accessor("cursor")
	int hologrammenu$getCursor();

	@Accessor("selectCursor")
	int hologrammenu$getSelectCursor();
}
