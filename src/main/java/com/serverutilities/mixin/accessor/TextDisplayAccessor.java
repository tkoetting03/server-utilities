package com.serverutilities.mixin.accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("getText")
	Component serverutilities$getText();

	@Invoker("setText")
	void serverutilities$setText(Component text);

	@Invoker("getLineWidth")
	int serverutilities$getLineWidth();

	@Invoker("setLineWidth")
	void serverutilities$setLineWidth(int lineWidth);

	@Invoker("setFlags")
	void serverutilities$setFlags(byte flags);
}
