package com.hologrammenu.mixin.accessor;

import com.mojang.math.Transformation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setBillboardConstraints")
	void hologrammenu$setBillboardConstraints(Display.BillboardConstraints constraints);

	@Invoker("setTransformation")
	void hologrammenu$setTransformation(Transformation transformation);

	@Invoker("createTransformation")
	static Transformation hologrammenu$createTransformation(SynchedEntityData entityData) {
		throw new AssertionError();
	}
}
