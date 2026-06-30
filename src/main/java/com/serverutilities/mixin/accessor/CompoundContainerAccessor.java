package com.serverutilities.mixin.accessor;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {
	@Accessor("container1")
	Container serverutilities$getContainer1();

	@Accessor("container2")
	Container serverutilities$getContainer2();
}
