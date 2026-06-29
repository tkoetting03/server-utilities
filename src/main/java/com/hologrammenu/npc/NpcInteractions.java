package com.hologrammenu.npc;

import com.hologrammenu.text.TextFormats;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

public final class NpcInteractions {
	private NpcInteractions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() || !NpcHelper.isNpc(entity)) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof LivingEntity living)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer && NpcEditMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			NpcConfig config = NpcConfig.read(living);
			sendDialogue(player, config.dialogue());
			if (config.containerEnabled() && player instanceof ServerPlayer serverPlayer) {
				NpcMenuOpener.openNpc(serverPlayer, living);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.SUCCESS;
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (NpcHelper.isNpc(entity) && player instanceof ServerPlayer serverPlayer && NpcEditMode.isActive(serverPlayer)) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static void sendDialogue(net.minecraft.world.entity.player.Player player, String dialogue) {
		if (dialogue == null || dialogue.isBlank()) {
			return;
		}
		for (String line : dialogue.split("\\R")) {
			if (!line.isBlank()) {
				player.sendSystemMessage(TextFormats.toComponent(translateAmpersandFormatting(line)));
			}
		}
	}

	private static String translateAmpersandFormatting(String value) {
		StringBuilder translated = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character == '&' && index + 1 < value.length() && isFormattingCode(value.charAt(index + 1))) {
				translated.append('§').append(Character.toLowerCase(value.charAt(index + 1)));
				index++;
			} else {
				translated.append(character);
			}
		}
		return translated.toString();
	}

	private static boolean isFormattingCode(char character) {
		char normalized = Character.toLowerCase(character);
		return (normalized >= '0' && normalized <= '9')
			|| (normalized >= 'a' && normalized <= 'f')
			|| (normalized >= 'k' && normalized <= 'o')
			|| normalized == 'r';
	}
}
