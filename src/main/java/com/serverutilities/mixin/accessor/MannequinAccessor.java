package com.serverutilities.mixin.accessor;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {
	@Invoker("setProfile")
	void serverutilities$setProfile(ResolvableProfile profile);

	@Invoker("setImmovable")
	void serverutilities$setImmovable(boolean immovable);

	@Invoker("setHideDescription")
	void serverutilities$setHideDescription(boolean hideDescription);
}
