package com.hologrammenu.client.mixin.accessor;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
	@Accessor("xpos")
	void hologrammenu$setXpos(double xpos);

	@Accessor("ypos")
	void hologrammenu$setYpos(double ypos);

	@Accessor("ignoreFirstMove")
	void hologrammenu$setIgnoreFirstMove(boolean ignoreFirstMove);
}
