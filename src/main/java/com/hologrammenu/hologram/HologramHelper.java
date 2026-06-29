package com.hologrammenu.hologram;

import com.hologrammenu.HologramMenuMod;
import com.hologrammenu.mixin.accessor.DisplayAccessor;
import com.hologrammenu.mixin.accessor.TextDisplayAccessor;
import com.hologrammenu.storage.StorageMenuHologramLabels;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Brightness;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.MutableComponent;
import com.hologrammenu.text.TextFormats;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HologramHelper {
	public static final double WAND_MAX_DISTANCE = 2.0D;
	private static final double DEFAULT_REMOVE_RADIUS = 6.0D;
	public static final double EDIT_MAX_DISTANCE = 5.0D;
	private static final double EDIT_LOOK_DOT_THRESHOLD = 0.92D;
	private static final double EDIT_RAY_DISTANCE = 2.0D;
	private static final double LINE_SPACING = 0.28D;
	private static final int TRANSPARENT_BACKGROUND = 0x00000000;
	private static final byte HOLOGRAM_TEXT_FLAGS = Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH;
	private static final float HOLOGRAM_CULLING_WIDTH = 4.0F;
	private static final float HOLOGRAM_CULLING_HEIGHT = 1.0F;
	private static final float HOLOGRAM_VIEW_RANGE = 32.0F;

	private HologramHelper() {
	}

	public static Component toMultiLineComponent(String text) {
		if (text == null || text.isBlank()) {
			return Component.empty();
		}

		MutableComponent combined = Component.empty();
		// Split the payload by the literal "\n" delimiter sent by the UI
		String[] lines = text.split("\\\\n");

		for (int i = 0; i < lines.length; i++) {
			// Parse each line's unique styling individually
			combined.append(TextFormats.toComponent(lines[i]));

			// Append an actual Minecraft newline character between lines
			if (i < lines.length - 1) {
				combined.append(Component.literal("\n"));
			}
		}

		return combined;
	}

	public static boolean isHologram(Entity entity) {
		if (!(entity instanceof Display.TextDisplay)) {
			return false;
		}

		return entity.entityTags().contains(HologramMenuMod.HOLOGRAM_TAG);
	}

	public static boolean isStorageLabel(Entity entity) {
		if (!(entity instanceof Display.TextDisplay display)) {
			return false;
		}
		return display.entityTags().contains(StorageMenuHologramLabels.STORAGE_LABEL_TAG);
	}

	public static boolean isEditableHologram(Entity entity) {
		return isHologram(entity) && !isStorageLabel(entity);
	}

	public static Display.TextDisplay createStorageLabel(ServerLevel level, Vec3 position, Component text) {
		return createStorageLabel(level, position, text, HologramScale.DEFAULT);
	}

	public static Display.TextDisplay createStorageLabel(ServerLevel level, Vec3 position, Component text, float scale) {
		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.setPos(position.x, position.y, position.z);
		display.setNoGravity(true);
		display.setInvulnerable(true);
		display.addTag(StorageMenuHologramLabels.STORAGE_LABEL_TAG);
		setText(display, text, scale);
		level.addFreshEntity(display);
		return display;
	}

	public static Display.TextDisplay create(ServerLevel level, Vec3 position, Component text) {
		List<HologramLineStack.Line> lines = HologramLineStack.parseLegacyText(TextFormats.fromComponent(text), HologramScale.DEFAULT);
		return create(level, position, lines).getFirst();
	}

	public static List<Display.TextDisplay> create(ServerLevel level, Vec3 position, List<HologramLineStack.Line> lines) {
		return create(level, position, UUID.randomUUID(), lines);
	}

	public static List<Display.TextDisplay> create(ServerLevel level, Vec3 position, UUID groupId, List<HologramLineStack.Line> lines) {
		List<HologramLineStack.Line> normalized = HologramLineStack.normalize(lines);
		List<Display.TextDisplay> displays = new ArrayList<>();
		for (int index = 0; index < normalized.size(); index++) {
			HologramLineStack.Line line = normalized.get(index);
			Vec3 linePosition = positionForLine(position, normalized.size(), index, line);
			Display.TextDisplay display = createLine(level, linePosition, groupId, index, line);
			displays.add(display);
		}
		return displays;
	}

	private static Display.TextDisplay createLine(ServerLevel level, Vec3 position, UUID groupId, int index, HologramLineStack.Line line) {
		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.setPos(position.x, position.y, position.z);
		display.setNoGravity(true);
		display.setInvulnerable(true);
		display.addTag(HologramMenuMod.HOLOGRAM_TAG);
		HologramLineStack.writeLineTags(display, groupId, index);

		setText(display, TextFormats.toComponent(line.text()), line.scale());
		level.addFreshEntity(display);
		HologramSync.track(level, display);
		return display;
	}

	public static void replaceGroup(ServerLevel level, Display.TextDisplay selected, List<HologramLineStack.Line> lines) {
		UUID groupId = HologramLineStack.groupId(selected);
		List<Display.TextDisplay> group = groupId == null ? List.of(selected) : findGroup(level, groupId);
		Vec3 anchor = anchorPosition(group.isEmpty() ? List.of(selected) : group);
		for (Display.TextDisplay display : group) {
			HologramSync.untrack(level, display);
			display.discard();
		}
		create(level, anchor, groupId == null ? UUID.randomUUID() : groupId, lines);
	}

	public static void removeGroup(ServerLevel level, Display.TextDisplay selected) {
		UUID groupId = HologramLineStack.groupId(selected);
		List<Display.TextDisplay> group = groupId == null ? List.of(selected) : findGroup(level, groupId);
		for (Display.TextDisplay display : group) {
			HologramSync.untrack(level, display);
			display.discard();
		}
	}

	public static List<Display.TextDisplay> findGroup(ServerLevel level, UUID groupId) {
		List<Display.TextDisplay> group = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof Display.TextDisplay display
				&& isEditableHologram(display)
				&& groupId.equals(HologramLineStack.groupId(display))) {
				group.add(display);
			}
		}
		group.sort(Comparator.comparingInt(HologramLineStack::lineIndex));
		return group;
	}

	private static Vec3 positionForLine(Vec3 anchor, int lineCount, int index, HologramLineStack.Line line) {
		return anchor.add(0.0D, (lineCount - 1 - index) * LINE_SPACING + line.heightOffset(), 0.0D);
	}

	private static Vec3 anchorPosition(List<Display.TextDisplay> displays) {
		return displays.stream()
			.max(Comparator.comparingInt(HologramLineStack::lineIndex))
			.map(Entity::position)
			.orElse(Vec3.ZERO);
	}

	public static void setText(Display.TextDisplay display, Component text) {
		setText(display, text, HologramScale.getScale(display));
	}

	public static void setText(Display.TextDisplay display, Component text, float scale) {
		TextDisplayAccessor accessor = (TextDisplayAccessor) display;
		accessor.hologrammenu$setText(text);
		if (accessor.hologrammenu$getLineWidth() <= 0) {
			accessor.hologrammenu$setLineWidth(200);
		}
		configureShaderFriendlyDisplay(display);

		DisplayAccessor displayAccessor = (DisplayAccessor) display;
		displayAccessor.hologrammenu$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		HologramScale.apply(display, scale);
	}

	private static void configureShaderFriendlyDisplay(Display.TextDisplay display) {
		display.setTextOpacity((byte)0xFF);
		display.setBackgroundColor(TRANSPARENT_BACKGROUND);
		display.setFlags(HOLOGRAM_TEXT_FLAGS);
		display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		display.setShadowRadius(0.0F);
		display.setShadowStrength(0.0F);
		display.setViewRange(HOLOGRAM_VIEW_RANGE);
		display.setWidth(HOLOGRAM_CULLING_WIDTH);
		display.setHeight(HOLOGRAM_CULLING_HEIGHT);
	}

	public static Vec3 pickPlacementPosition(Player player, double maxDistance) {
		HitResult hit = player.pick(maxDistance, 1.0F, false);
		return switch (hit.getType()) {
			case BLOCK -> offsetFromBlockFace((BlockHitResult) hit);
			case ENTITY -> hit.getLocation();
			default -> player.getEyePosition().add(player.getLookAngle().scale(maxDistance));
		};
	}

	private static Vec3 offsetFromBlockFace(BlockHitResult blockHit) {
		Direction direction = blockHit.getDirection();
		Vec3 offset = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()).scale(0.05D);
		return blockHit.getLocation().add(offset);
	}

	public static Vec3 placementPosition(Player player) {
		return pickPlacementPosition(player, WAND_MAX_DISTANCE);
	}

	public static boolean canEdit(Player player, Entity entity) {
		return player.distanceToSqr(entity) <= EDIT_MAX_DISTANCE * EDIT_MAX_DISTANCE;
	}

	public static Optional<Display.TextDisplay> findLookAtHologram(Player player, java.util.function.Predicate<Entity> isTargetHologram, double maxDistance) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double maxDistanceSq = maxDistance * maxDistance;
		double maxRayDistanceSq = EDIT_RAY_DISTANCE * EDIT_RAY_DISTANCE;

		Display.TextDisplay best = null;
		double bestScore = maxRayDistanceSq;

		for (Entity entity : player.level().getEntities(EntityType.TEXT_DISPLAY, player.getBoundingBox().inflate(maxDistance), isTargetHologram)) {
			if (!(entity instanceof Display.TextDisplay display)) {
				continue;
			}

			if (!canEdit(player, display)) {
				continue;
			}

			Vec3 target = display.position().add(0.0D, display.getBbHeight() * 0.5D, 0.0D);
			Vec3 toTarget = target.subtract(eye);
			double distanceSq = toTarget.lengthSqr();
			if (distanceSq > maxDistanceSq || distanceSq < 1.0E-4D) {
				continue;
			}

			double dot = look.dot(toTarget.normalize());
			if (dot < EDIT_LOOK_DOT_THRESHOLD) {
				continue;
			}

			double rayDistanceSq = distanceToRaySquared(eye, look, target);
			if (rayDistanceSq > bestScore) {
				continue;
			}

			bestScore = rayDistanceSq;
			best = display;
		}

		return Optional.ofNullable(best);
	}

	private static double distanceToRaySquared(Vec3 origin, Vec3 direction, Vec3 point) {
		Vec3 offset = point.subtract(origin);
		double alongRay = offset.dot(direction);
		if (alongRay <= 0.0D) {
			return Double.MAX_VALUE;
		}

		return offset.subtract(direction.scale(alongRay)).lengthSqr();
	}

	public static Optional<Display.TextDisplay> findNearest(ServerLevel level, Vec3 origin, double radius) {
		AABB box = AABB.ofSize(origin, radius * 2.0D, radius * 2.0D, radius * 2.0D);
		List<Display.TextDisplay> matches = level.getEntities(EntityType.TEXT_DISPLAY, box,
			HologramHelper::isHologram)
			.stream()
			.sorted(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(origin)))
			.toList();

		if (matches.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(matches.getFirst());
	}

	public static boolean removeNearest(ServerLevel level, Vec3 origin) {
		Optional<Display.TextDisplay> nearest = findNearest(level, origin, DEFAULT_REMOVE_RADIUS);
		nearest.ifPresent(display -> removeGroup(level, display));
		return nearest.isPresent();
	}

	public static List<Display.TextDisplay> listNearby(ServerLevel level, Vec3 origin, double radius) {
		AABB box = AABB.ofSize(origin, radius * 2.0D, radius * 2.0D, radius * 2.0D);
		List<UUID> seenGroups = new ArrayList<>();
		List<Display.TextDisplay> displays = level.getEntities(EntityType.TEXT_DISPLAY, box, HologramHelper::isHologram)
			.stream()
			.sorted(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(origin)))
			.toList();
		List<Display.TextDisplay> roots = new ArrayList<>();
		for (Display.TextDisplay display : displays) {
			UUID groupId = HologramLineStack.groupId(display);
			if (groupId == null) {
				roots.add(display);
				continue;
			}
			if (seenGroups.contains(groupId)) {
				continue;
			}
			seenGroups.add(groupId);
			roots.add(display);
		}
		return roots;
	}
}
