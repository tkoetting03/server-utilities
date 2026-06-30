package com.serverutilities.itemstyler;

import com.serverutilities.ServerUtilitiesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
	public static final MenuType<ItemStylerMenu> ITEM_STYLER = Registry.register(
		BuiltInRegistries.MENU,
		ServerUtilitiesMod.id("item_styler"),
		new MenuType<>(ItemStylerMenu::new, FeatureFlags.VANILLA_SET)
	);

	private ModMenuTypes() {
	}

	public static void register() {
	}
}
