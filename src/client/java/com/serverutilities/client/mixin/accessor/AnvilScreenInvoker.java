package com.serverutilities.client.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AnvilScreen.class)
public interface AnvilScreenInvoker {
	@Invoker("onNameChanged")
	void serverutilities$onNameChanged(String name);
}
