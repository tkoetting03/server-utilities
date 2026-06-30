package com.serverutilities.client.screen;

import com.serverutilities.client.config.ClientSettings;
import com.serverutilities.client.screen.widget.ModPanelLayout;
import com.serverutilities.client.screen.widget.UiLayoutHelper;
import com.serverutilities.client.screen.widget.UiScaleText;
import com.serverutilities.client.storage.StorageMenuClientPermissions;
import com.serverutilities.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.Vec3;

public final class NpcToolScreen extends Screen {
	private final Screen parent;
	private final Vec3 placementPosition;
	private EditBox skinField;
	private EditBox nameField;
	private Button typeButton;
	private Button professionButton;

	public NpcToolScreen(Screen parent, Vec3 placementPosition) {
		super(Component.translatable("screen.serverutilities.npc_tool.title"));
		this.parent = parent;
		this.placementPosition = placementPosition;
	}

	@Override
	protected void init() {
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = ModPanelLayout.stackHeight(4, rowHeight, rowGap) + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		int y = contentTop;

		typeButton = Button.builder(typeLabel(), press -> {
			ClientSettings.npcKind = ClientSettings.npcKind == ClientSettings.NpcKind.VILLAGER
				? ClientSettings.NpcKind.PLAYER
				: ClientSettings.NpcKind.VILLAGER;
			press.setMessage(typeLabel());
			updateFieldVisibility();
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(typeButton);
		y += rowHeight + rowGap;

		professionButton = Button.builder(professionLabel(), press -> {
			ClientSettings.npcProfessionIndex = (ClientSettings.npcProfessionIndex + 1) % ClientSettings.NPC_PROFESSIONS.length;
			press.setMessage(professionLabel());
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(professionButton);
		y += rowHeight + rowGap;

		skinField = new EditBox(this.font, fieldX, y, contentWidth, rowHeight, Component.translatable("screen.serverutilities.npc_tool.skin"));
		skinField.setMaxLength(64);
		skinField.setValue(ClientSettings.npcSkinName);
		skinField.setHint(Component.translatable("screen.serverutilities.npc_tool.skin_hint"));
		skinField.setResponder(value -> ClientSettings.npcSkinName = value);
		addRenderableWidget(skinField);
		y += rowHeight + rowGap;

		nameField = new EditBox(this.font, fieldX, y, contentWidth, rowHeight, Component.translatable("screen.serverutilities.npc_tool.display_name"));
		nameField.setMaxLength(64);
		nameField.setValue(ClientSettings.npcDisplayName);
		nameField.setHint(Component.translatable("screen.serverutilities.npc_tool.display_name_hint"));
		nameField.setResponder(value -> ClientSettings.npcDisplayName = value);
		addRenderableWidget(nameField);
		y += rowHeight + sectionGap;

		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.serverutilities.shop.confirm"), press -> placeNpc())
			.bounds(fieldX, y, half, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), press -> onCancel())
			.bounds(fieldX + half + rowGap, y, half, rowHeight).build());

		updateFieldVisibility();
	}

	private void placeNpc() {
		if (!StorageMenuClientPermissions.canEdit()) {
			return;
		}

		String npcType = ClientSettings.npcKind == ClientSettings.NpcKind.PLAYER ? "player" : "villager";
		String skinName = skinField.getValue().trim();
		if ("player".equals(npcType) && skinName.isEmpty()) {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("hud.serverutilities.npc.missing_skin"));
			}
			return;
		}

		ClientPlayNetworking.send(new ModPackets.NpcPlacePayload(
			placementPosition.x,
			placementPosition.y,
			placementPosition.z,
			npcType,
			skinName,
			ClientSettings.currentNpcProfessionId(),
			nameField.getValue().trim()
		));
		onCancel();
	}

	private void updateFieldVisibility() {
		boolean player = ClientSettings.npcKind == ClientSettings.NpcKind.PLAYER;
		skinField.visible = player;
		skinField.active = player;
		professionButton.visible = !player;
		professionButton.active = !player;
	}

	private Component typeLabel() {
		return Component.translatable(
			ClientSettings.npcKind == ClientSettings.NpcKind.PLAYER
				? "screen.serverutilities.npc_tool.type_player"
				: "screen.serverutilities.npc_tool.type_villager"
		);
	}

	private Component professionLabel() {
		ResourceKey<VillagerProfession> key = ClientSettings.NPC_PROFESSIONS[
			Math.floorMod(ClientSettings.npcProfessionIndex, ClientSettings.NPC_PROFESSIONS.length)
		];
		return Component.translatable("screen.serverutilities.npc_tool.profession")
			.append(" ")
			.append(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(key).value().name());
	}

	private void onCancel() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public void onClose() {
		onCancel();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = ModPanelLayout.stackHeight(4, rowHeight, rowGap) + sectionGap + rowHeight;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.serverutilities.npc_tool.hint"),
			this.width / 2,
			ModPanelLayout.hintY(contentTop),
			0xA0A0A0
		);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
