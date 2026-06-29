package com.hologrammenu.npc;

import com.hologrammenu.hologram.HologramHelper;
import com.hologrammenu.hologram.HologramScale;
import com.hologrammenu.text.TextFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NpcHologramLabels {
	public static final String NPC_LABEL_TAG = "hologrammenu:npc_label";
	private static final String OWNER_PREFIX = "hologrammenu:npc_label_owner:";
	private static final double BASE_Y_OFFSET = 0.35D;
	private static final double LINE_SPACING = 0.28D;

	private NpcHologramLabels() {
	}

	public static void sync(ServerLevel level, LivingEntity npc, List<NpcHologramStack.Entry> entries) {
		remove(level, npc);
		List<NpcHologramStack.Entry> stack = NpcHologramStack.withNameText(entries, NpcHologramStack.readStyledName(npc));
		if (!NpcHologramStack.hasExtraHolograms(stack)) {
			applyVanillaName(npc, stack);
			return;
		}

		npc.setCustomName(null);
		npc.setCustomNameVisible(false);

		List<Display.TextDisplay> displays = new ArrayList<>();
		for (int index = 0; index < stack.size(); index++) {
			NpcHologramStack.Entry entry = stack.get(index);
			if (entry.text() == null || entry.text().isBlank()) {
				continue;
			}
			Vec3 position = positionForIndex(npc, stack.size(), index, entry.scale());
			displays.add(createLabel(level, npc.getUUID(), position, TextFormats.toComponent(entry.text()), entry.scale()));
		}
		ACTIVE.put(npc.getUUID(), displays);
	}

	public static void tick(ServerLevel level, LivingEntity npc) {
		List<Display.TextDisplay> displays = ACTIVE.get(npc.getUUID());
		if (displays == null || displays.isEmpty()) {
			return;
		}

		List<NpcHologramStack.Entry> stack = NpcHologramStack.read(npc);
		if (!NpcHologramStack.hasExtraHolograms(stack)) {
			remove(level, npc);
			applyVanillaName(npc, stack);
			return;
		}

		int displayIndex = 0;
		for (int index = 0; index < stack.size(); index++) {
			NpcHologramStack.Entry entry = stack.get(index);
			if (entry.text() == null || entry.text().isBlank()) {
				continue;
			}
			if (displayIndex >= displays.size()) {
				sync(level, npc, stack);
				return;
			}
			Display.TextDisplay display = displays.get(displayIndex);
			if (display.isRemoved()) {
				sync(level, npc, stack);
				return;
			}
			Vec3 position = positionForIndex(npc, stack.size(), index, entry.scale());
			display.setPos(position.x, position.y, position.z);
			displayIndex++;
		}
	}

	public static void remove(ServerLevel level, LivingEntity npc) {
		ACTIVE.remove(npc.getUUID());
		String ownerTag = OWNER_PREFIX + npc.getUUID();
		AABB box = npc.getBoundingBox().inflate(2.0D, 4.0D, 2.0D);
		for (Display.TextDisplay display : level.getEntities(EntityType.TEXT_DISPLAY, box, NpcHologramLabels::isNpcLabel)) {
			if (display.entityTags().contains(ownerTag)) {
				display.discard();
			}
		}
	}

	public static boolean isNpcLabel(Display.TextDisplay display) {
		return display.entityTags().contains(NPC_LABEL_TAG);
	}

	public static void tickTracked(ServerLevel level) {
		for (UUID owner : new ArrayList<>(ACTIVE.keySet())) {
			LivingEntity npc = null;
			for (var entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity living && living.getUUID().equals(owner) && NpcHelper.isNpc(living)) {
					npc = living;
					break;
				}
			}
			if (npc == null) {
				ACTIVE.remove(owner);
				continue;
			}
			tick(level, npc);
		}
	}

	private static final Map<UUID, List<Display.TextDisplay>> ACTIVE = new HashMap<>();

	private static Display.TextDisplay createLabel(
		ServerLevel level,
		UUID owner,
		Vec3 position,
		Component text,
		float scale
	) {
		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.setPos(position.x, position.y, position.z);
		display.setNoGravity(true);
		display.setInvulnerable(true);
		display.addTag(NPC_LABEL_TAG);
		display.addTag(OWNER_PREFIX + owner);
		HologramHelper.setText(display, text, HologramScale.clamp(scale));
		level.addFreshEntity(display);
		return display;
	}

	private static Vec3 positionForIndex(LivingEntity npc, int stackSize, int index, float scale) {
		double baseY = npc.getY() + npc.getBbHeight() + BASE_Y_OFFSET;
		double spacing = LINE_SPACING * Math.max(0.75F, scale);
		double y = baseY + (stackSize - 1 - index) * spacing;
		return new Vec3(npc.getX(), y, npc.getZ());
	}

	private static void applyVanillaName(LivingEntity npc, List<NpcHologramStack.Entry> stack) {
		String styled = NpcHologramStack.readStyledName(npc);
		if (styled == null || styled.isBlank()) {
			for (NpcHologramStack.Entry entry : stack) {
				if (entry.isName() && entry.text() != null && !entry.text().isBlank()) {
					styled = entry.text();
					break;
				}
			}
		}
		if (styled == null || styled.isBlank()) {
			npc.setCustomName(null);
			npc.setCustomNameVisible(false);
			return;
		}
		npc.setCustomName(TextFormats.toComponent(styled.trim()));
		npc.setCustomNameVisible(true);
	}
}
