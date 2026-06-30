package com.serverutilities.mixin;

import com.serverutilities.anvil.AnvilLoreSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public class ItemCombinerMenuMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void serverutilities$clearAnvilLore(Player player, CallbackInfo ci) {
		if ((Object) this instanceof AnvilMenu && player instanceof ServerPlayer serverPlayer) {
			AnvilLoreSession.clear(serverPlayer);
		}
	}
}
