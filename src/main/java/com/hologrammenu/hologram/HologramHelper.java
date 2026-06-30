package com.hologrammenu.hologram;

import com.hologrammenu.HologramMenuMod;
import com.hologrammenu.mixin.accessor.DisplayAccessor;
import com.hologrammenu.mixin.accessor.TextDisplayAccessor;
import com.hologrammenu.storage.StorageMenuHologramLabels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
	private static final int TRANSPARENT_BACKGROUND = 0x00000000;
	private static final byte HOLOGRAM_TEXT_FLAGS = Display.TextDisplay.FLAG_SHADOW;
	private static final float HOLOGRAM_CULLING_WIDTH = 4.0F;
	private static final float HOLOGRAM_CULLING_HEIGHT = 1.0F;
	private static final float HOLOGRAM_VIEW_RANGE = 32.0F;
	private static final String ASSOCIATED_BLOCK_TAG_PREFIX = HologramMenuMod.MOD_ID + ":hologram_base_block:";
	private static final String ASSOCIATED_ENTITY_TAG_PREFIX = HologramMenuMod.MOD_ID + ":hologram_base_entity:";
	private static final double ASSOCIATED_ITEM_SEARCH_RADIUS = 1.25D;
	private static final int ASSOCIATED_BLOCK_SEARCH_RADIUS = 1;
	private static final int ASSOCIATED_BLOCK_SEARCH_DOWN = 64;
	private static final int ASSOCIATED_BLOCK_SEARCH_UP = 2;

	public record PlacementTarget(Vec3 position, Optional<BlockPos> blockPos) {
		public PlacementTarget {
			blockPos = blockPos == null ? Optional.empty() : blockPos.map(BlockPos::immutable);
		}
	}

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
		return create(level, position, groupId, lines, resolveAssociation(level, position));
	}

	private static List<Display.TextDisplay> create(ServerLevel level, Vec3 position, UUID groupId, List<HologramLineStack.Line> lines, Association association) {
		List<HologramLineStack.Line> normalized = HologramLineStack.normalize(lines);
		float groupHeightOffset = groupHeightOffset(normalized);
		List<Display.TextDisplay> displays = new ArrayList<>();
		for (int index = 0; index < normalized.size(); index++) {
			HologramLineStack.Line line = normalized.get(index);
			Vec3 linePosition = positionForLine(position, normalized, index, line);
			Display.TextDisplay display = createLine(level, linePosition, groupId, index, line, groupHeightOffset, association);
			displays.add(display);
		}
		return displays;
	}

	private static Display.TextDisplay createLine(
		ServerLevel level,
		Vec3 position,
		UUID groupId,
		int index,
		HologramLineStack.Line line,
		float groupHeightOffset,
		Association association
	) {
		Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		display.setPos(position.x, position.y, position.z);
		display.setNoGravity(true);
		display.setInvulnerable(true);
		display.addTag(HologramMenuMod.HOLOGRAM_TAG);
		HologramLineStack.writeLineTags(display, groupId, index, groupHeightOffset, line.heightOffset());
		applyAssociationTags(display, association);

		setText(display, TextFormats.toComponent(line.text()), line.scale(), line.seeThroughWalls());
		level.addFreshEntity(display);
		HologramSync.track(level, display);
		return display;
	}

	public static void replaceGroup(ServerLevel level, Display.TextDisplay selected, List<HologramLineStack.Line> lines) {
		UUID groupId = HologramLineStack.groupId(selected);
		List<Display.TextDisplay> group = groupId == null ? List.of(selected) : findGroup(level, groupId);
		Vec3 anchor = baseAnchorPosition(group.isEmpty() ? List.of(selected) : group);
		Association association = readAssociation(selected).orElse(null);
		for (Display.TextDisplay display : group) {
			HologramSync.untrack(level, display);
			display.discard();
		}
		create(level, anchor, groupId == null ? UUID.randomUUID() : groupId, lines, association);
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

	public static Optional<Display.TextDisplay> findEditableByAssociatedBlock(ServerLevel level, BlockPos blockPos, Player player) {
		if (blockPos == null) {
			return Optional.empty();
		}
		Display.TextDisplay best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof Display.TextDisplay display) || !isEditableHologram(display)) {
				continue;
			}
			Optional<BlockPos> associated = associatedBlock(display);
			if (associated.isEmpty() || !associated.get().equals(blockPos)) {
				continue;
			}
			if (player != null && !canEdit(player, display)) {
				continue;
			}
			double distance = player == null ? Vec3.atCenterOf(blockPos).distanceToSqr(display.position()) : player.distanceToSqr(Vec3.atCenterOf(blockPos));
			if (distance < bestDistance) {
				bestDistance = distance;
				best = display;
			}
		}
		return Optional.ofNullable(best);
	}

	private static Vec3 positionForLine(Vec3 anchor, List<HologramLineStack.Line> lines, int index, HologramLineStack.Line line) {
		return anchor.add(0.0D, HologramLineStack.stackOffsetForLine(lines, index) + line.heightOffset(), 0.0D);
	}

	public static void tagAssociatedBlock(Entity entity, BlockPos pos) {
		applyAssociationTags(entity, new Association(pos == null ? null : pos.immutable(), null));
	}

	public static Optional<BlockPos> associatedBlock(Entity entity) {
		return readAssociation(entity).flatMap(Association::block);
	}

	public static Optional<UUID> associatedEntityUuid(Entity entity) {
		return readAssociation(entity).flatMap(Association::entityUuid);
	}

	private static Optional<Association> readAssociation(Entity entity) {
		BlockPos block = null;
		UUID entityUuid = null;
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(ASSOCIATED_BLOCK_TAG_PREFIX)) {
				try {
					block = BlockPos.of(Long.parseLong(tag.substring(ASSOCIATED_BLOCK_TAG_PREFIX.length())));
				} catch (NumberFormatException ignored) {
					block = null;
				}
			} else if (tag.startsWith(ASSOCIATED_ENTITY_TAG_PREFIX)) {
				try {
					entityUuid = UUID.fromString(tag.substring(ASSOCIATED_ENTITY_TAG_PREFIX.length()));
				} catch (IllegalArgumentException ignored) {
					entityUuid = null;
				}
			}
		}
		return block == null && entityUuid == null ? Optional.empty() : Optional.of(new Association(block, entityUuid));
	}

	private static void applyAssociationTags(Entity entity, Association association) {
		entity.entityTags().removeIf(tag -> tag.startsWith(ASSOCIATED_BLOCK_TAG_PREFIX) || tag.startsWith(ASSOCIATED_ENTITY_TAG_PREFIX));
		if (association == null) {
			return;
		}
		association.block().ifPresent(pos -> entity.addTag(ASSOCIATED_BLOCK_TAG_PREFIX + pos.asLong()));
		association.entityUuid().ifPresent(uuid -> entity.addTag(ASSOCIATED_ENTITY_TAG_PREFIX + uuid));
	}

	private static Association resolveAssociation(ServerLevel level, Vec3 position) {
		Optional<ItemEntity> item = nearestAssociatedItem(level, position);
		if (item.isPresent()) {
			return new Association(null, item.get().getUUID());
		}
		return new Association(nearestAssociatedBlock(level, position).orElse(null), null);
	}

	private static Optional<ItemEntity> nearestAssociatedItem(ServerLevel level, Vec3 position) {
		AABB searchBox = new AABB(position, position).inflate(ASSOCIATED_ITEM_SEARCH_RADIUS);
		ItemEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ItemEntity item : level.getEntities(EntityType.ITEM, searchBox, item -> !item.isRemoved())) {
			double distance = item.position().distanceToSqr(position);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = item;
			}
		}
		return Optional.ofNullable(best);
	}

	private static Optional<BlockPos> nearestAssociatedBlock(ServerLevel level, Vec3 position) {
		BlockPos origin = BlockPos.containing(position);
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dy = ASSOCIATED_BLOCK_SEARCH_UP; dy >= -ASSOCIATED_BLOCK_SEARCH_DOWN; dy--) {
			for (int dx = -ASSOCIATED_BLOCK_SEARCH_RADIUS; dx <= ASSOCIATED_BLOCK_SEARCH_RADIUS; dx++) {
				for (int dz = -ASSOCIATED_BLOCK_SEARCH_RADIUS; dz <= ASSOCIATED_BLOCK_SEARCH_RADIUS; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					VoxelShape shape = state.getShape(level, pos);
					if (shape.isEmpty()) {
						continue;
					}
					double distance = Vec3.atCenterOf(pos).distanceToSqr(position);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = pos.immutable();
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	private record Association(BlockPos rawBlock, UUID rawEntityUuid) {
		Optional<BlockPos> block() {
			return Optional.ofNullable(rawBlock);
		}

		Optional<UUID> entityUuid() {
			return Optional.ofNullable(rawEntityUuid);
		}
	}

	private static float groupHeightOffset(List<HologramLineStack.Line> lines) {
		if (lines.isEmpty()) {
			return 0.0F;
		}
		float total = 0.0F;
		for (HologramLineStack.Line line : lines) {
			total += line.heightOffset();
		}
		return HologramLineStack.clampHeightOffset(total / lines.size());
	}

	private static Vec3 anchorPosition(List<Display.TextDisplay> displays) {
		return displays.stream()
			.max(Comparator.comparingInt(HologramLineStack::lineIndex))
			.map(Entity::position)
			.orElse(Vec3.ZERO);
	}

	private static Vec3 baseAnchorPosition(List<Display.TextDisplay> displays) {
		Vec3 anchor = anchorPosition(displays);
		List<HologramLineStack.Line> lines = HologramLineStack.readGroup(displays);
		return lines.isEmpty() ? anchor : anchor.subtract(0.0D, lines.getLast().heightOffset(), 0.0D);
	}

	public static void setText(Display.TextDisplay display, Component text) {
		setText(display, text, HologramScale.getScale(display), HologramLineStack.isSeeThrough(display));
	}

	public static void setText(Display.TextDisplay display, Component text, float scale) {
		setText(display, text, scale, true);
	}

	public static void setText(Display.TextDisplay display, Component text, float scale, boolean seeThroughWalls) {
		TextDisplayAccessor accessor = (TextDisplayAccessor) display;
		accessor.hologrammenu$setText(text);
		if (accessor.hologrammenu$getLineWidth() <= 0) {
			accessor.hologrammenu$setLineWidth(200);
		}
		configureShaderFriendlyDisplay(display, seeThroughWalls);

		DisplayAccessor displayAccessor = (DisplayAccessor) display;
		displayAccessor.hologrammenu$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		HologramScale.apply(display, scale);
	}

	private static void configureShaderFriendlyDisplay(Display.TextDisplay display, boolean seeThroughWalls) {
		display.setTextOpacity((byte)0xFF);
		display.setBackgroundColor(TRANSPARENT_BACKGROUND);
		display.setFlags((byte)(HOLOGRAM_TEXT_FLAGS | (seeThroughWalls ? Display.TextDisplay.FLAG_SEE_THROUGH : 0)));
		display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		display.setShadowRadius(0.0F);
		display.setShadowStrength(0.0F);
		display.setViewRange(HOLOGRAM_VIEW_RANGE);
		display.setWidth(HOLOGRAM_CULLING_WIDTH);
		display.setHeight(HOLOGRAM_CULLING_HEIGHT);
	}

	public static Vec3 pickPlacementPosition(Player player, double maxDistance) {
		return pickPlacementTarget(player, maxDistance).position();
	}

	public static PlacementTarget pickPlacementTarget(Player player, double maxDistance) {
		HitResult hit = player.pick(maxDistance, 1.0F, false);
		return switch (hit.getType()) {
			case BLOCK -> {
				BlockHitResult blockHit = (BlockHitResult) hit;
				yield new PlacementTarget(offsetFromBlockFace(blockHit), Optional.of(blockHit.getBlockPos()));
			}
			case ENTITY -> new PlacementTarget(hit.getLocation(), Optional.empty());
			default -> new PlacementTarget(player.getEyePosition().add(player.getLookAngle().scale(maxDistance)), Optional.empty());
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
		double maxDistanceSq = EDIT_MAX_DISTANCE * EDIT_MAX_DISTANCE;
		if (player.distanceToSqr(entity) <= maxDistanceSq) {
			return true;
		}
		Optional<BlockPos> associatedBlock = associatedBlock(entity);
		if (associatedBlock.isEmpty() && entity.level() instanceof ServerLevel level) {
			associatedBlock = nearestAssociatedBlock(level, entity.position());
		}
		return associatedBlock.isPresent() && player.distanceToSqr(Vec3.atCenterOf(associatedBlock.get())) <= maxDistanceSq;
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
