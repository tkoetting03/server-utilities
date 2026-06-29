package com.hologrammenu.mixin.accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("getText")
	Component hologrammenu$getText();

	@Invoker("setText")
	void hologrammenu$setText(Component text);

	@Invoker("getLineWidth")
	int hologrammenu$getLineWidth();

	@Invoker("setLineWidth")
	void hologrammenu$setLineWidth(int lineWidth);

	@Invoker("setFlags")
	void hologrammenu$setFlags(byte flags);
}
