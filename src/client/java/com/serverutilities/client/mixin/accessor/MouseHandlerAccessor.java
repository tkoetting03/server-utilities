package com.serverutilities.client.mixin.accessor;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
	@Accessor("xpos")
	void serverutilities$setXpos(double xpos);

	@Accessor("ypos")
	void serverutilities$setYpos(double ypos);

	@Accessor("ignoreFirstMove")
	void serverutilities$setIgnoreFirstMove(boolean ignoreFirstMove);
}
