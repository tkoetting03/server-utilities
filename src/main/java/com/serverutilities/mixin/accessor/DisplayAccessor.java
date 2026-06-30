package com.serverutilities.mixin.accessor;

import com.mojang.math.Transformation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setBillboardConstraints")
	void serverutilities$setBillboardConstraints(Display.BillboardConstraints constraints);

	@Invoker("setTransformation")
	void serverutilities$setTransformation(Transformation transformation);

	@Invoker("createTransformation")
	static Transformation serverutilities$createTransformation(SynchedEntityData entityData) {
		throw new AssertionError();
	}
}
