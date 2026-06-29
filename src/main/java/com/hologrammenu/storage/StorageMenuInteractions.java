package com.hologrammenu.storage;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public final class StorageMenuInteractions {
	private StorageMenuInteractions() {
	}

	public static void register() {
		StorageMenuBlockProtection.register();

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world.isClientSide() || !(world instanceof ServerLevel level)) {
				return;
			}
			StorageMenuHologramLabels.remove(level, pos);
			StorageMenuBlockStore.clear(level, pos);
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}

			BlockPos pos = hitResult.getBlockPos();
			if (!StorageMenuBlockStore.get(level, pos).map(data -> data.definition().enabled()).orElse(false)) {
				return InteractionResult.PASS;
			}

			if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity) {
				return InteractionResult.PASS;
			}

			StorageMenuOpener.openBlock(serverPlayer, pos);
			return InteractionResult.SUCCESS;
		});
	}

	public static boolean handleClick(AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input, ServerPlayer player) {
		VirtualStorageContainer container = VirtualStorageContainer.asVirtual(StorageMenuLocator.extractContainer(menu));
		if (container == null) {
			return false;
		}

		if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
			return false;
		}

		Slot slot = menu.slots.get(slotIndex);
		if (slot.container != container) {
			return input == ContainerInput.QUICK_MOVE && !slot.getItem().isEmpty();
		}

		StorageMenuSlotConfig config = container.definition().slot(slot.getContainerSlot());
		int menuSlot = slot.getContainerSlot();

		if (!menu.getCarried().isEmpty()) {
			return true;
		}

		if (input != ContainerInput.PICKUP) {
			return true;
		}

		if (ShopTransactions.tryPurchase(player, (ServerLevel) player.level(), container, menuSlot)) {
			return true;
		}

		if (button == 1 && config.hasSubMenu()) {
			StorageMenuNavigation.openSubMenu(player, container.viewContext(), config.subMenuId());
			return true;
		}

		if (!config.blocksInteraction()) {
			return false;
		}

		if (input == ContainerInput.PICKUP) {
			switch (config.type()) {
				case COMMAND -> runCommand(player, config.command());
				case LINK -> {
					if (config.hasSubMenu()) {
						StorageMenuNavigation.openSubMenu(player, container.viewContext(), config.subMenuId());
					} else {
						sendFeedback(player, "screen.hologrammenu.storage_menu.link_missing");
					}
				}
				case BACK -> StorageMenuNavigation.openBack(player);
				case CLOSE -> player.closeContainer();
				default -> {
				}
			}
		}

		return true;
	}

	public static void runCommand(ServerPlayer player, String command) {
		if (command == null || command.isBlank()) {
			return;
		}

		player.level().getServer().getCommands().performPrefixedCommand(
			player.createCommandSourceStack(),
			command.trim()
		);
	}

	public static void sendFeedback(ServerPlayer player, String translationKey) {
		player.sendSystemMessage(Component.translatable(translationKey));
	}
}
