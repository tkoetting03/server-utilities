package com.hologrammenu.mixin.accessor;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {
	@Invoker("setProfile")
	void hologrammenu$setProfile(ResolvableProfile profile);

	@Invoker("setImmovable")
	void hologrammenu$setImmovable(boolean immovable);

	@Invoker("setHideDescription")
	void hologrammenu$setHideDescription(boolean hideDescription);
}
