package com.hologrammenu.npc;

import com.hologrammenu.network.StorageMenuNetworkHandler;
import com.hologrammenu.storage.ShopDefinition;
import com.hologrammenu.storage.StorageMenuBlockData;
import com.hologrammenu.storage.StorageMenuChrome;
import com.hologrammenu.storage.StorageMenuDefinition;
import com.hologrammenu.storage.StorageMenuFactory;
import com.hologrammenu.storage.StorageMenuNavigation;
import com.hologrammenu.storage.StorageMenuSizes;
import com.hologrammenu.storage.StorageMenuViewContext;
import com.hologrammenu.storage.StorageSubMenuManager;
import com.hologrammenu.text.TextFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.UUID;

public final class NpcMenuOpener {
	private NpcMenuOpener() {
	}

	public static void openNpc(ServerPlayer player, LivingEntity npc) {
		NpcConfig config = NpcConfig.read(npc);
		if (!config.containerEnabled()) {
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		UUID entityUuid = npc.getUUID();
		NpcMenuStore.ensureEnabled(level, entityUuid, config.containerSize());

		Optional<StorageMenuBlockData> data = NpcMenuStore.get(level, entityUuid);
		if (data.isEmpty() || !data.get().definition().enabled()) {
			return;
		}

		StorageMenuDefinition definition = data.get().definition();
		StorageMenuViewContext viewContext = StorageMenuViewContext.forNpc(npc.getId());
		Container validitySource = new NpcEntityValidityContainer(npc);
		StorageMenuNavigation.onDirectOpen(player);
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				String customName = NpcHelper.readDisplayName(npc);
				if (!customName.isBlank()) {
					return TextFormats.toComponent(customName);
				}
				return definition.displayTitle().orElse(Component.translatable("screen.hologrammenu.npc_options.container_title"));
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
				return buildMenu(
					level,
					viewContext,
					definition,
					data.get().shop(),
					syncId,
					playerInventory,
					validitySource,
					player
				);
			}
		});
	}

	public static int resolveContainerSize(ServerLevel level, LivingEntity npc) {
		NpcConfig config = NpcConfig.read(npc);
		return NpcMenuStore.get(level, npc.getUUID())
			.filter(data -> data.definition().enabled())
			.map(data -> data.definition().containerSize())
			.orElse(config.containerSize());
	}

	private static AbstractContainerMenu buildMenu(
		ServerLevel level,
		StorageMenuViewContext viewContext,
		StorageMenuDefinition definition,
		ShopDefinition shop,
		int syncId,
		Inventory playerInventory,
		Container validitySource,
		ServerPlayer serverPlayer
	) {
		definition = StorageMenuChrome.applyRuntimeChrome(definition, serverPlayer);
		StorageMenuNetworkHandler.sendContext(serverPlayer, viewContext);
		StorageMenuNetworkHandler.sendNavigationState(serverPlayer);
		StorageMenuNetworkHandler.sendNpcShopState(serverPlayer, viewContext.npcEntityId(), shop);
		StorageMenuNavigation.setCurrentView(serverPlayer, viewContext);
		return StorageMenuFactory.create(level, viewContext, definition, shop, syncId, playerInventory, validitySource, (BlockEntity) null);
	}

	static StorageMenuDefinition resolveDefinition(ServerLevel level, StorageMenuViewContext viewContext, LivingEntity npc, int containerSize) {
		if (viewContext.isRoot()) {
			return NpcMenuStore.get(level, npc.getUUID())
				.map(StorageMenuBlockData::definition)
				.filter(StorageMenuDefinition::enabled)
				.orElse(StorageMenuDefinition.empty(containerSize));
		}
		return StorageSubMenuManager.get(level, viewContext.subMenuId())
			.orElse(StorageMenuDefinition.empty(containerSize).withEnabled(true));
	}

	public static void openView(ServerPlayer player, LivingEntity npc, StorageMenuViewContext viewContext, int containerSize) {
		ServerLevel level = (ServerLevel) player.level();
		StorageMenuDefinition definition = resolveDefinition(level, viewContext, npc, containerSize);
		ShopDefinition shop = viewContext.isRoot()
			? NpcMenuStore.get(level, npc.getUUID()).map(StorageMenuBlockData::shop).orElse(ShopDefinition.EMPTY)
			: ShopDefinition.EMPTY;
		Container validitySource = new NpcEntityValidityContainer(npc);
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return definition.displayTitle().orElse(Component.translatable("screen.hologrammenu.npc_options.container_title"));
			}

			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
				return buildMenu(level, viewContext, definition, shop, syncId, playerInventory, validitySource, player);
			}
		});
		StorageMenuNavigation.setCurrentView(player, viewContext);
	}
}
