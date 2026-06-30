package com.serverutilities.mixin;

import com.serverutilities.anvil.AnvilLoreSession;
import com.serverutilities.mixin.accessor.ItemCombinerMenuAccessor;
import com.serverutilities.storage.StorageMenuItemLore;
import com.serverutilities.text.TextFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Inject(method = "validateName", at = @At("HEAD"), cancellable = true)
	private static void serverutilities$validateFormattedName(String name, CallbackInfoReturnable<String> cir) {
		if (name == null) {
			cir.setReturnValue(null);
			return;
		}

		String filtered = TextFormats.filterAnvilName(name);
		if (TextFormats.plainTextForLengthCheck(filtered).length() > 50) {
			cir.setReturnValue(null);
			return;
		}

		cir.setReturnValue(filtered);
	}

	@Redirect(
		method = {"setItemName", "createResult"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
		)
	)
	private static MutableComponent serverutilities$literalWithFormatting(String text) {
		return TextFormats.toComponent(text);
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void serverutilities$applyPendingLore(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		Player player = ((ItemCombinerMenuAccessor) menu).serverutilities$getPlayer();
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		if (!AnvilLoreSession.hasPending(serverPlayer)) {
			return;
		}

		List<String> loreLines = AnvilLoreSession.get(serverPlayer);

		ItemStack result = menu.getSlot(2).getItem();
		if (result.isEmpty()) {
			return;
		}

		menu.getSlot(2).set(StorageMenuItemLore.withLore(result, loreLines));
	}
}
