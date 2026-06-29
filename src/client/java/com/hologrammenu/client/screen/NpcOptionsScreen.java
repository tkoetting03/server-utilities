package com.hologrammenu.client.screen;

import com.hologrammenu.client.config.ClientSettings;
import com.hologrammenu.client.npc.NpcClientConfigStore;
import com.hologrammenu.client.npc.NpcEditorSessionState;
import com.hologrammenu.client.screen.widget.ModPanelLayout;
import com.hologrammenu.client.screen.widget.NpcRadiusSlider;
import com.hologrammenu.client.screen.widget.UiLayoutHelper;
import com.hologrammenu.client.screen.widget.UiScaleText;
import com.hologrammenu.client.screen.HeadPresetPickerOverlay;
import com.hologrammenu.client.screen.TextStyleOverlay;
import com.hologrammenu.client.screen.TextStylePanelPositions;
import com.hologrammenu.client.screen.NpcHologramStackOverlay;
import com.hologrammenu.client.screen.widget.LabeledFieldLayout;
import com.hologrammenu.client.screen.widget.UiScale;
import com.hologrammenu.head.HeadPresetIds;
import com.hologrammenu.particle.ParticlePresetCatalog;
import com.hologrammenu.particle.ParticlePresetEntry;
import com.hologrammenu.npc.NpcHologramStack;
import com.hologrammenu.network.ModPackets;
import com.hologrammenu.npc.NpcConfig;
import com.hologrammenu.npc.NpcHelper;
import com.hologrammenu.text.TextFormats;
import com.hologrammenu.storage.StorageMenuSizes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NpcOptionsScreen extends Screen {
	private static final int BASE_CONTENT_ROWS = 11;
	private static final int MAX_DIALOGUE_LINES = 4;
	private static final Map<Integer, String> SESSION_SKIN_BY_ENTITY = new HashMap<>();

	private final int entityId;
	private boolean playerNpc;
	private EditBox nameField;
	private EditBox skinField;
	private final List<EditBox> dialogueFields = new ArrayList<>();
	private Button professionButton;
	private Button headFollowButton;
	private Button containerButton;
	private Button containerSizeButton;
	private Button openContainerButton;
	private int professionIndex;
	private boolean headFollowEnabled;
	private float headFollowRadius;
	private boolean containerEnabled;
	private int containerSize;
	private boolean particleEffectEnabled;
	private String pendingParticleEffectId = "";
	private String pendingName = "";
	private String styledDisplayName = "";
	private String pendingSkin = "";
	private final List<String> pendingDialogueLines = new ArrayList<>();
	private List<NpcHologramStack.Entry> hologramStack = NpcHologramStack.defaults("");
	private TextStyleOverlay nameStyleOverlay;
	private NpcHologramStackOverlay hologramStackOverlay;
	private Button hologramsButton;
	private Button headPresetsButton;
	private HeadPresetPickerOverlay headPresetPickerOverlay;
	private Button particleEffectButton;
	private Button particlePresetsButton;
	private ParticlePresetPickerOverlay particlePresetPickerOverlay;
	private boolean deleting;

	public NpcOptionsScreen(int entityId) {
		super(Component.translatable("screen.hologrammenu.npc_options.title"));
		this.entityId = entityId;
		this.headFollowRadius = NpcConfig.DEFAULT_HEAD_RADIUS;
		this.containerSize = StorageMenuSizes.SINGLE_CHEST;
	}

	@Override
	protected void init() {
		boolean restoreNameStyle = nameStyleOverlay != null && nameStyleOverlay.isOpen();
		var savedNameDraft = restoreNameStyle ? nameStyleOverlay.getDraft() : null;
		boolean restoreHologramStack = hologramStackOverlay != null && hologramStackOverlay.isOpen();
		java.util.List<NpcHologramStack.Entry> savedHologramStack = restoreHologramStack
			? new java.util.ArrayList<>(hologramStack)
			: null;
		boolean restoreHeadPresets = headPresetPickerOverlay != null && headPresetPickerOverlay.isOpen();
		boolean restoreParticlePresets = particlePresetPickerOverlay != null && particlePresetPickerOverlay.isOpen();
		if (nameStyleOverlay != null) {
			nameStyleOverlay.dispose();
		}
		if (hologramStackOverlay != null && restoreHologramStack) {
			hologramStackOverlay.dispose();
		}
		if (headPresetPickerOverlay != null && restoreHeadPresets) {
			headPresetPickerOverlay.close();
		}
		if (particlePresetPickerOverlay != null && restoreParticlePresets) {
			particlePresetPickerOverlay.close();
		}

		if (nameField != null) {
			captureFieldValues();
		} else {
			LivingEntity entity = findEntity();
			if (entity != null) {
				playerNpc = entity instanceof Mannequin;
				loadFromEntity(entity);
				styledDisplayName = NpcHelper.readDisplayName(entity);
				pendingName = TextFormats.parse(styledDisplayName).text();
				hologramStack = new java.util.ArrayList<>(NpcHologramStack.read(entity));
				pendingSkin = resolveInitialSkin(entity);
				loadDialogueLines(resolveInitialDialogue(entity));
			}
		}
		if (pendingDialogueLines.isEmpty()) {
			pendingDialogueLines.add("");
		}

		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentHeight = contentHeight(rowHeight, rowGap, sectionGap);
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight);
		int y = contentTop;

		int styleWidth = UiScale.s(40);
		int nameGap = UiScale.s(4);
		int nameFieldWidth = contentWidth - styleWidth - nameGap;
		nameField = new EditBox(this.font, fieldX, y, nameFieldWidth, rowHeight, Component.translatable("screen.hologrammenu.npc_tool.display_name"));
		nameField.setMaxLength(64);
		nameField.setValue(pendingName);
		nameField.addFormatter((visible, start) -> TextFormats.editBoxFormat(styledDisplayName, visible, start));
		nameField.setResponder(value -> {
			styledDisplayName = TextFormats.parse(styledDisplayName).withText(value).serialize();
			hologramStack = NpcHologramStack.withNameText(hologramStack, styledDisplayName);
		});
		addRenderableWidget(nameField);
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.hologram_options.style"), press -> toggleNameStyle())
			.bounds(fieldX + nameFieldWidth + nameGap, y, styleWidth, rowHeight).build());
		y += rowHeight + rowGap;

		hologramsButton = Button.builder(Component.translatable("screen.hologrammenu.npc_options.holograms"), press -> toggleHologramStack())
			.bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(hologramsButton);
		y += rowHeight + rowGap;

		skinField = new EditBox(this.font, fieldX, y, contentWidth, rowHeight, Component.translatable("screen.hologrammenu.npc_tool.skin"));
		skinField.setMaxLength(64);
		skinField.setValue(pendingSkin);
		skinField.setHint(Component.translatable("screen.hologrammenu.npc_tool.skin_hint"));
		skinField.setResponder(this::rememberSkinValue);
		addRenderableWidget(skinField);

		headPresetsButton = Button.builder(Component.translatable("screen.hologrammenu.head_presets.button"), press -> toggleHeadPresets())
			.bounds(fieldX, y + rowHeight + rowGap, contentWidth, rowHeight).build();
		addRenderableWidget(headPresetsButton);

		professionButton = Button.builder(professionLabel(), press -> {
			professionIndex = (professionIndex + 1) % ClientSettings.NPC_PROFESSIONS.length;
			press.setMessage(professionLabel());
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(professionButton);
		y += (rowHeight + rowGap) * 2;

		dialogueFields.clear();
		int dialogueLabelWidth = UiScale.s(54);
		int dialogueFieldWidth = contentWidth - dialogueLabelWidth - rowGap;
		for (int index = 0; index < pendingDialogueLines.size(); index++) {
			EditBox field = new EditBox(
				this.font,
				fieldX + dialogueLabelWidth + rowGap,
				y,
				dialogueFieldWidth,
				rowHeight,
				Component.translatable("screen.hologrammenu.npc_options.dialogue_line", index + 1)
			);
			field.setMaxLength(256);
			field.setValue(pendingDialogueLines.get(index));
			field.setHint(Component.translatable("screen.hologrammenu.npc_options.dialogue_hint"));
			final int lineIndex = index;
			field.setResponder(value -> {
				while (pendingDialogueLines.size() <= lineIndex) {
					pendingDialogueLines.add("");
				}
				pendingDialogueLines.set(lineIndex, value);
			});
			dialogueFields.add(field);
			addRenderableWidget(field);
			y += rowHeight + rowGap;
		}

		int halfDialogue = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.npc_options.dialogue_add"), press -> {
			captureFieldValues();
			if (pendingDialogueLines.size() < MAX_DIALOGUE_LINES) {
				pendingDialogueLines.add("");
			}
			rebuildNpcWidgets();
		}).bounds(fieldX, y, halfDialogue, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.npc_options.dialogue_remove"), press -> {
			captureFieldValues();
			if (pendingDialogueLines.size() > 1) {
				pendingDialogueLines.remove(pendingDialogueLines.size() - 1);
			} else {
				pendingDialogueLines.set(0, "");
			}
			rebuildNpcWidgets();
		}).bounds(fieldX + halfDialogue + rowGap, y, halfDialogue, rowHeight).build());
		y += rowHeight + rowGap;

		headFollowButton = Button.builder(headFollowLabel(), press -> {
			headFollowEnabled = !headFollowEnabled;
			press.setMessage(headFollowLabel());
			rememberToggles();
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(headFollowButton);
		y += rowHeight + rowGap;

		addRenderableWidget(new NpcRadiusSlider(
			fieldX,
			y,
			contentWidth,
			Component.translatable("screen.hologrammenu.npc_options.head_radius"),
			headFollowRadius,
			() -> headFollowRadius,
			value -> {
				headFollowRadius = value;
				rememberToggles();
			}
		));
		y += rowHeight + rowGap;

		containerButton = Button.builder(containerLabel(), press -> {
			containerEnabled = !containerEnabled;
			press.setMessage(containerLabel());
			updateContainerControls();
			rememberToggles();
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(containerButton);
		y += rowHeight + rowGap;

		int half = ModPanelLayout.columnWidth(contentWidth, 2, rowGap);
		containerSizeButton = Button.builder(containerSizeLabel(), press -> {
			containerSize = containerSize == StorageMenuSizes.DOUBLE_CHEST
				? StorageMenuSizes.SINGLE_CHEST
				: StorageMenuSizes.DOUBLE_CHEST;
			press.setMessage(containerSizeLabel());
			rememberToggles();
		}).bounds(fieldX, y, half, rowHeight).build();
		addRenderableWidget(containerSizeButton);

		openContainerButton = Button.builder(Component.translatable("screen.hologrammenu.npc_options.open_container"), press -> openContainer())
			.bounds(fieldX + half + rowGap, y, half, rowHeight).build();
		addRenderableWidget(openContainerButton);
		y += rowHeight + rowGap;

		particleEffectButton = Button.builder(particleEffectLabel(), press -> {
			particleEffectEnabled = !particleEffectEnabled;
			press.setMessage(particleEffectLabel());
			rememberToggles();
		}).bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(particleEffectButton);
		y += rowHeight + rowGap;

		particlePresetsButton = Button.builder(Component.translatable("screen.hologrammenu.particle_presets.button"), press -> toggleParticlePresets())
			.bounds(fieldX, y, contentWidth, rowHeight).build();
		addRenderableWidget(particlePresetsButton);
		y += rowHeight + sectionGap;

		int third = ModPanelLayout.columnWidth(contentWidth, 3, rowGap);
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.npc_options.save"), press -> save())
			.bounds(fieldX, y, third, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.hologrammenu.npc_options.delete"), press -> delete())
			.bounds(fieldX + third + rowGap, y, third, rowHeight).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), press -> onClose())
			.bounds(fieldX + (third + rowGap) * 2, y, third, rowHeight).build());

		updateNpcTypeControls();
		updateContainerControls();
		ensureNameStyleOverlay();
		if (restoreNameStyle && savedNameDraft != null) {
			nameStyleOverlay.openWithDraft(savedNameDraft);
		}
		ensureHologramStackOverlay();
		if (restoreHologramStack && savedHologramStack != null) {
			hologramStackOverlay.open(savedHologramStack);
		}
		ensureHeadPresetPickerOverlay();
		if (restoreHeadPresets) {
			headPresetPickerOverlay.open();
		}
		ensureParticlePresetPickerOverlay();
		if (restoreParticlePresets) {
			particlePresetPickerOverlay.open();
		}
	}

	private int contentHeight(int rowHeight, int rowGap, int sectionGap) {
		return ModPanelLayout.stackHeight(BASE_CONTENT_ROWS + Math.max(1, pendingDialogueLines.size()), rowHeight, rowGap)
			+ sectionGap
			+ rowHeight;
	}

	private void rebuildNpcWidgets() {
		clearWidgets();
		init();
	}

	private void captureFieldValues() {
		if (nameField != null) {
			pendingName = nameField.getValue();
			styledDisplayName = TextFormats.parse(styledDisplayName).withText(pendingName).serialize();
			hologramStack = NpcHologramStack.withNameText(hologramStack, styledDisplayName);
		}
		if (skinField != null) {
			rememberSkinValue(skinField.getValue());
		}
		if (!dialogueFields.isEmpty()) {
			pendingDialogueLines.clear();
			for (EditBox field : dialogueFields) {
				pendingDialogueLines.add(field.getValue());
			}
			if (pendingDialogueLines.isEmpty()) {
				pendingDialogueLines.add("");
			}
		}
		rememberToggles();
	}

	private void rememberToggles() {
		NpcEditorSessionState.remember(
			entityId,
			headFollowEnabled,
			headFollowRadius,
			containerEnabled,
			containerSize,
			particleEffectEnabled,
			pendingParticleEffectId
		);
	}

	private String resolveInitialDialogue(LivingEntity entity) {
		return NpcClientConfigStore.resolve(entityId, entity).dialogue();
	}

	private void loadDialogueLines(String dialogue) {
		pendingDialogueLines.clear();
		if (dialogue != null && !dialogue.isBlank()) {
			for (String line : dialogue.split("\\R", -1)) {
				if (pendingDialogueLines.size() >= MAX_DIALOGUE_LINES) {
					break;
				}
				pendingDialogueLines.add(line);
			}
		}
		if (pendingDialogueLines.isEmpty()) {
			pendingDialogueLines.add("");
		}
	}

	private String serializeDialogueLines() {
		List<String> lines = new ArrayList<>();
		for (String line : pendingDialogueLines) {
			lines.add(line == null ? "" : line.trim());
		}
		while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
			lines.remove(lines.size() - 1);
		}
		return String.join("\n", lines);
	}

	private String resolveInitialSkin(LivingEntity entity) {
		String synced = NpcClientConfigStore.skinName(entityId);
		if (SESSION_SKIN_BY_ENTITY.containsKey(entityId)) {
			String session = SESSION_SKIN_BY_ENTITY.get(entityId);
			return session == null || session.isBlank() ? synced : session;
		}
		return synced.isBlank() ? NpcHelper.readSkinName(entity) : synced;
	}

	private void rememberSkinValue(String value) {
		pendingSkin = value == null ? "" : value;
		SESSION_SKIN_BY_ENTITY.put(entityId, pendingSkin);
	}

	private void loadFromEntity(LivingEntity entity) {
		NpcEditorSessionState.ToggleState session = NpcEditorSessionState.toggles(entityId);
		if (session != null) {
			headFollowEnabled = session.headFollowEnabled();
			headFollowRadius = session.headFollowRadius();
			containerEnabled = session.containerEnabled();
			containerSize = session.containerSize();
			particleEffectEnabled = session.particleEffectEnabled();
			pendingParticleEffectId = session.particleEffectId();
		} else {
			NpcConfig config = NpcClientConfigStore.resolve(entityId, entity);
			headFollowEnabled = config.headFollowEnabled();
			headFollowRadius = config.headFollowRadius();
			containerEnabled = config.containerEnabled();
			containerSize = config.containerSize();
			particleEffectEnabled = config.particleEffectEnabled();
			pendingParticleEffectId = config.particleEffectId();
		}
		if (entity instanceof Villager villager) {
			String professionId = NpcHelper.readProfessionId(villager);
			professionIndex = 0;
			for (int i = 0; i < ClientSettings.NPC_PROFESSIONS.length; i++) {
				if (ClientSettings.NPC_PROFESSIONS[i].identifier().toString().equals(professionId)) {
					professionIndex = i;
					break;
				}
			}
		}
	}

	private LivingEntity findEntity() {
		if (this.minecraft == null || this.minecraft.level == null) {
			return null;
		}
		var entity = this.minecraft.level.getEntity(entityId);
		return entity instanceof LivingEntity living ? living : null;
	}

	private void openContainer() {
		if (!containerEnabled) {
			return;
		}
		if (playerNpc && skinField != null && skinField.getValue().trim().isEmpty()) {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("hud.hologrammenu.npc.missing_skin"));
			}
			return;
		}
		if (findEntity() == null) {
			onClose();
			return;
		}
		captureFieldValues();
		sendUpdatePayload();
		ClientPlayNetworking.send(new ModPackets.NpcOpenMenuPayload(entityId));
		onClose();
	}

	private void save() {
		LivingEntity entity = findEntity();
		if (entity == null) {
			onClose();
			return;
		}
		if (playerNpc && skinField != null && skinField.getValue().trim().isEmpty()) {
			if (this.minecraft.player != null) {
				this.minecraft.player.sendOverlayMessage(Component.translatable("hud.hologrammenu.npc.missing_skin"));
			}
			return;
		}
		sendUpdatePayload();
		rememberToggles();
		onClose();
	}

	private void sendUpdatePayload() {
		styledDisplayName = TextFormats.parse(styledDisplayName).withText(nameField.getValue()).serialize();
		hologramStack = NpcHologramStack.withNameText(hologramStack, styledDisplayName);
		rememberSkinValue(skinField == null ? "" : skinField.getValue());
		if (!dialogueFields.isEmpty()) {
			pendingDialogueLines.clear();
			for (EditBox field : dialogueFields) {
				pendingDialogueLines.add(field.getValue());
			}
		}
		ClientPlayNetworking.send(new ModPackets.NpcEditPayload(
			entityId,
			"update",
			styledDisplayName,
			pendingSkin.trim(),
			ClientSettings.NPC_PROFESSIONS[Math.floorMod(professionIndex, ClientSettings.NPC_PROFESSIONS.length)].identifier().toString(),
			serializeDialogueLines(),
			headFollowEnabled,
			headFollowRadius,
			containerEnabled,
			containerSize,
			NpcHologramStack.serialize(hologramStack),
			particleEffectEnabled,
			pendingParticleEffectId
		));
	}

	private void delete() {
		deleting = true;
		SESSION_SKIN_BY_ENTITY.remove(entityId);
		NpcEditorSessionState.clear(entityId);
		ClientPlayNetworking.send(new ModPackets.NpcEditPayload(
			entityId,
			"delete",
			"",
			"",
			"",
			"",
			false,
			NpcConfig.DEFAULT_HEAD_RADIUS,
			false,
			StorageMenuSizes.SINGLE_CHEST,
			"",
			false,
			""
		));
		onClose();
	}

	private void updateNpcTypeControls() {
		boolean player = playerNpc;
		skinField.visible = player;
		skinField.active = player;
		headPresetsButton.visible = player;
		headPresetsButton.active = player;
		professionButton.visible = !player;
		professionButton.active = !player;
	}

	private void updateContainerControls() {
		boolean enabled = containerEnabled;
		containerSizeButton.visible = enabled;
		containerSizeButton.active = enabled;
		openContainerButton.visible = enabled;
		openContainerButton.active = enabled;
	}

	private Component professionLabel() {
		ResourceKey<VillagerProfession> key = ClientSettings.NPC_PROFESSIONS[
			Math.floorMod(professionIndex, ClientSettings.NPC_PROFESSIONS.length)
		];
		return Component.translatable("screen.hologrammenu.npc_tool.profession")
			.append(" ")
			.append(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(key).value().name());
	}

	private Component headFollowLabel() {
		return Component.translatable(
			headFollowEnabled
				? "screen.hologrammenu.npc_options.head_follow_on"
				: "screen.hologrammenu.npc_options.head_follow_off"
		);
	}

	private Component containerLabel() {
		return Component.translatable(
			containerEnabled
				? "screen.hologrammenu.npc_options.container_on"
				: "screen.hologrammenu.npc_options.container_off"
		);
	}

	private Component containerSizeLabel() {
		return Component.translatable(
			containerSize == StorageMenuSizes.DOUBLE_CHEST
				? "screen.hologrammenu.storage_menu.size.double"
				: "screen.hologrammenu.storage_menu.size.single"
		);
	}

	private Component particleEffectLabel() {
		String effectName = ParticlePresetCatalog.get(pendingParticleEffectId)
			.map(ParticlePresetEntry::name)
			.orElseGet(() -> pendingParticleEffectId == null || pendingParticleEffectId.isBlank()
				? Component.translatable("screen.hologrammenu.particle_presets.none").getString()
				: pendingParticleEffectId);
		return Component.translatable(
			particleEffectEnabled
				? "screen.hologrammenu.npc_options.particle_on"
				: "screen.hologrammenu.npc_options.particle_off",
			effectName
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int sectionGap = ModPanelLayout.SECTION_GAP;
		int contentTop = ModPanelLayout.centeredContentTop(this.height, contentHeight(rowHeight, rowGap, sectionGap));
		UiScaleText.drawCentered(graphics, this.font, this.title, this.width / 2, ModPanelLayout.titleY(contentTop), 0xFFFFFF);
		UiScaleText.drawCentered(
			graphics,
			this.font,
			Component.translatable("screen.hologrammenu.npc_options.hint"),
			this.width / 2,
			ModPanelLayout.hintY(contentTop),
			0xA0A0A0
		);
		drawDialogueLabels(graphics, contentTop);
	}

	private void drawDialogueLabels(GuiGraphicsExtractor graphics, int contentTop) {
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int rowHeight = UiLayoutHelper.buttonHeight(this.font);
		int rowGap = ModPanelLayout.ROW_GAP;
		int y = contentTop + (rowHeight + rowGap) * 4;
		for (int index = 0; index < pendingDialogueLines.size(); index++) {
			UiScaleText.draw(
				graphics,
				this.font,
				Component.translatable("screen.hologrammenu.npc_options.dialogue_line", index + 1),
				fieldX,
				y + UiScale.s(4),
				0xA0A0A0
			);
			y += rowHeight + rowGap;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
	}

	@Override
	public void onClose() {
		if (!deleting) {
			captureFieldValues();
		}
		if (nameStyleOverlay != null) {
			nameStyleOverlay.dispose();
		}
		if (hologramStackOverlay != null) {
			hologramStackOverlay.close();
		}
		if (headPresetPickerOverlay != null) {
			headPresetPickerOverlay.close();
		}
		if (particlePresetPickerOverlay != null) {
			particlePresetPickerOverlay.close();
		}
		super.onClose();
	}

	private void toggleNameStyle() {
		ensureNameStyleOverlay();
		styledDisplayName = TextFormats.parse(styledDisplayName).withText(nameField.getValue()).serialize();
		int fieldY = nameField.getY();
		int[] position = TextStylePanelPositions.besideField(this, nameField.getX(), nameField.getWidth(), fieldY);
		nameStyleOverlay.toggle(styledDisplayName, position[0], position[1]);
	}

	private void ensureNameStyleOverlay() {
		if (nameStyleOverlay != null) {
			return;
		}
		nameStyleOverlay = new TextStyleOverlay(
			this,
			() -> nameField.getValue(),
			serialized -> {
				styledDisplayName = TextFormats.normalize(serialized);
				String plain = TextFormats.parse(styledDisplayName).text();
				if (!plain.equals(nameField.getValue())) {
					nameField.setValue(plain);
				}
				hologramStack = NpcHologramStack.withNameText(hologramStack, styledDisplayName);
			},
			() -> TextStylePanelPositions.besideField(this, nameField.getX(), nameField.getWidth(), nameField.getY())
		);
	}

	private void toggleHologramStack() {
		ensureHologramStackOverlay();
		styledDisplayName = TextFormats.parse(styledDisplayName).withText(nameField.getValue()).serialize();
		hologramStack = NpcHologramStack.withNameText(hologramStack, styledDisplayName);
		hologramStackOverlay.toggle(hologramStack);
	}

	private void ensureHologramStackOverlay() {
		if (hologramStackOverlay != null) {
			return;
		}
		hologramStackOverlay = new NpcHologramStackOverlay(
			this,
			() -> TextFormats.parse(styledDisplayName).withText(nameField.getValue()).serialize(),
			stack -> hologramStack = new java.util.ArrayList<>(stack),
			this::hologramPanelPosition
		);
	}

	private int[] hologramPanelPosition() {
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int anchorY = hologramsButton != null
			? hologramsButton.getY()
			: nameField != null ? nameField.getY() : 0;
		return TextStylePanelPositions.besideField(this, fieldX, contentWidth, anchorY);
	}

	private void toggleHeadPresets() {
		ensureHeadPresetPickerOverlay();
		headPresetPickerOverlay.toggle();
	}

	private void ensureHeadPresetPickerOverlay() {
		if (headPresetPickerOverlay != null) {
			return;
		}
		headPresetPickerOverlay = new HeadPresetPickerOverlay(
			this,
			entry -> {
				if (skinField != null) {
					skinField.setValue(HeadPresetIds.encode(entry.id()));
					rememberSkinValue(skinField.getValue());
				}
			},
			this::headPresetPanelPosition
		);
	}

	private int[] headPresetPanelPosition() {
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int anchorY = headPresetsButton != null
			? headPresetsButton.getY()
			: skinField != null ? skinField.getY() : 0;
		return TextStylePanelPositions.besideField(this, fieldX, contentWidth, anchorY);
	}

	private void toggleParticlePresets() {
		ensureParticlePresetPickerOverlay();
		particlePresetPickerOverlay.toggle();
	}

	private void ensureParticlePresetPickerOverlay() {
		if (particlePresetPickerOverlay != null) {
			return;
		}
		particlePresetPickerOverlay = new ParticlePresetPickerOverlay(
			this,
			entry -> {
				pendingParticleEffectId = entry.isNone() ? "" : entry.id();
				particleEffectEnabled = !entry.isNone();
				if (particleEffectButton != null) {
					particleEffectButton.setMessage(particleEffectLabel());
				}
				rememberToggles();
			},
			this::particlePresetPanelPosition
		);
	}

	private int[] particlePresetPanelPosition() {
		int contentWidth = ModPanelLayout.screenContentWidth(this.width);
		int fieldX = ModPanelLayout.centeredX(this.width, contentWidth);
		int anchorY = particlePresetsButton != null
			? particlePresetsButton.getY()
			: particleEffectButton != null ? particleEffectButton.getY() : 0;
		return TextStylePanelPositions.besideField(this, fieldX, contentWidth, anchorY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (particlePresetPickerOverlay != null && particlePresetPickerOverlay.isOpen() && particlePresetPickerOverlay.mouseScrolled(scrollY)) {
			return true;
		}
		if (headPresetPickerOverlay != null && headPresetPickerOverlay.isOpen() && headPresetPickerOverlay.mouseScrolled(scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
