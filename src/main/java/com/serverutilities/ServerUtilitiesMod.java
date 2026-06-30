package com.serverutilities;

import com.serverutilities.command.ModCommands;
import com.serverutilities.hologram.HologramInteractions;
import com.serverutilities.hologram.HologramPlacementHandler;
import com.serverutilities.itemstyler.ModMenuTypes;
import com.serverutilities.npc.NpcHologramLabelHandler;
import com.serverutilities.npc.NpcHeadFollowHandler;
import com.serverutilities.npc.NpcInteractions;
import com.serverutilities.npc.NpcParticleHandler;
import com.serverutilities.npc.NpcPlacementHandler;
import com.serverutilities.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtilitiesMod implements ModInitializer {
	public static final String MOD_ID = "serverutilities";
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
		com.serverutilities.storage.StorageMenuInteractions.register();
		ModMenuTypes.register();
		NetworkHandler.registerServer();
		ModCommands.register();
		LOGGER.info("Server Utilities Mod initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
