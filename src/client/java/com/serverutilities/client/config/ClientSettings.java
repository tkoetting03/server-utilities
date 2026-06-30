package com.serverutilities.client.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public final class ClientSettings {
	public static boolean placementModeEnabled = false;
	public static boolean storagePlacementModeEnabled = false;
	public static boolean npcPlacementModeEnabled = false;
	public static boolean npcEditModeEnabled = false;
	public static boolean hologramEditModeEnabled = false;
	public static boolean styleWidgetEnabled = true;
	public static boolean containerEditorWidgetEnabled = true;
	public static String defaultPlacementText = "Hologram";

	public static NpcKind npcKind = NpcKind.VILLAGER;
	public static String npcSkinName = "";
	public static String npcDisplayName = "";
	public static int npcProfessionIndex = 0;

	public enum NpcKind {
		VILLAGER,
		PLAYER
	}

	public static final ResourceKey<VillagerProfession>[] NPC_PROFESSIONS = new ResourceKey[] {
		VillagerProfession.NONE,
		VillagerProfession.FARMER,
		VillagerProfession.LIBRARIAN,
		VillagerProfession.ARMORER,
		VillagerProfession.BUTCHER,
		VillagerProfession.CARTOGRAPHER,
		VillagerProfession.CLERIC,
		VillagerProfession.FISHERMAN,
		VillagerProfession.FLETCHER,
		VillagerProfession.LEATHERWORKER,
		VillagerProfession.MASON,
		VillagerProfession.SHEPHERD,
		VillagerProfession.TOOLSMITH,
		VillagerProfession.WEAPONSMITH
	};

	private ClientSettings() {
	}

	public static void apply(ClientConfig config) {
		defaultPlacementText = config.defaultPlacementText != null && !config.defaultPlacementText.isBlank()
			? config.defaultPlacementText
			: "Hologram";
		styleWidgetEnabled = config.styleWidgetEnabled == null || config.styleWidgetEnabled;
		containerEditorWidgetEnabled = config.containerEditorWidgetEnabled == null || config.containerEditorWidgetEnabled;
	}

	public static String currentNpcProfessionId() {
		int index = Math.floorMod(npcProfessionIndex, NPC_PROFESSIONS.length);
		return NPC_PROFESSIONS[index].identifier().toString();
	}
}
