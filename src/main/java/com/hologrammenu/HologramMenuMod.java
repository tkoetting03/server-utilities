package com.hologrammenu;

import com.hologrammenu.command.ModCommands;
import com.hologrammenu.hologram.HologramInteractions;
import com.hologrammenu.hologram.HologramPlacementHandler;
import com.hologrammenu.itemstyler.ModMenuTypes;
import com.hologrammenu.npc.NpcHologramLabelHandler;
import com.hologrammenu.npc.NpcHeadFollowHandler;
import com.hologrammenu.npc.NpcInteractions;
import com.hologrammenu.npc.NpcParticleHandler;
import com.hologrammenu.npc.NpcPlacementHandler;
import com.hologrammenu.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HologramMenuMod implements ModInitializer {
	public static final String MOD_ID = "hologrammenu";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String HOLOGRAM_TAG = MOD_ID + ":hologram";
	public static final String NPC_TAG = MOD_ID + ":npc";

	@Override
	public void onInitialize() {
		HologramInteractions.register();
		HologramPlacementHandler.register();
		NpcPlacementHandler.register();
		NpcInteractions.register();
		NpcHeadFollowHandler.register();
		NpcParticleHandler.register();
		NpcHologramLabelHandler.register();
		com.hologrammenu.storage.StorageMenuInteractions.register();
		ModMenuTypes.register();
		NetworkHandler.registerServer();
		ModCommands.register();
		LOGGER.info("Hologram Menu Mod initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
