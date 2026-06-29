package com.hologrammenu.storage;

import com.hologrammenu.mixin.accessor.CompoundContainerAccessor;
import net.minecraft.world.CompoundContainer;
import com.hologrammenu.mixin.accessor.DispenserMenuAccessor;
import com.hologrammenu.mixin.accessor.HopperMenuAccessor;
import com.hologrammenu.mixin.accessor.ShulkerBoxMenuAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.Optional;

public final class StorageMenuLocator {
	private StorageMenuLocator() {
	}

	public static boolean isSupportedMenu(AbstractContainerMenu menu) {
		return extractContainer(menu) != null;
	}

	public static int containerSize(AbstractContainerMenu menu) {
		Container container = extractContainer(menu);
		return container == null ? 0 : container.getContainerSize();
	}

	public static Optional<BlockPos> resolveBlockPos(AbstractContainerMenu menu) {
		Container container = extractContainer(menu);
		if (container == null) {
			return Optional.empty();
		}

		if (container instanceof VirtualStorageContainer virtual) {
			return Optional.of(virtual.blockPos());
		}

		return blockPosFromContainer(container);
	}

	public static Optional<BlockPos> resolveBlockPos(AbstractContainerMenu menu, BlockPos fallback) {
		Optional<BlockPos> resolved = resolveBlockPos(menu);
		if (resolved.isPresent()) {
			return resolved;
		}
		return fallback == null ? Optional.empty() : Optional.of(fallback);
	}

	public static Container extractContainer(AbstractContainerMenu menu) {
		if (menu instanceof ChestMenu chestMenu) {
			return chestMenu.getContainer();
		}
		if (menu instanceof HopperMenu hopperMenu) {
			return ((HopperMenuAccessor) hopperMenu).hologrammenu$getHopper();
		}
		if (menu instanceof ShulkerBoxMenu shulkerBoxMenu) {
			return ((ShulkerBoxMenuAccessor) shulkerBoxMenu).hologrammenu$getContainer();
		}
		if (menu instanceof DispenserMenu dispenserMenu) {
			return ((DispenserMenuAccessor) dispenserMenu).hologrammenu$getDispenser();
		}
		return null;
	}

	private static Optional<BlockPos> blockPosFromContainer(Container container) {
		if (container instanceof BlockEntity blockEntity) {
			return Optional.of(blockEntity.getBlockPos());
		}
		if (container instanceof CompoundContainer compoundContainer) {
			CompoundContainerAccessor compound = (CompoundContainerAccessor) compoundContainer;
			Container first = compound.hologrammenu$getContainer1();
			Optional<BlockPos> firstPos = blockPosFromContainer(first);
			if (firstPos.isPresent()) {
				return firstPos;
			}
			return blockPosFromContainer(compound.hologrammenu$getContainer2());
		}
		return Optional.empty();
	}

	public static boolean isHopperLike(Container container) {
		if (container instanceof HopperBlockEntity) {
			return true;
		}
		if (container instanceof CompoundContainer compoundContainer) {
			CompoundContainerAccessor compound = (CompoundContainerAccessor) compoundContainer;
			return isHopperLike(compound.hologrammenu$getContainer1())
				|| isHopperLike(compound.hologrammenu$getContainer2());
		}
		return false;
	}
}
