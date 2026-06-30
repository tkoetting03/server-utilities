package com.serverutilities.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ShopItemRules {
	private ShopItemRules() {
	}

	public static boolean isBlockedShopProduct(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.has(DataComponents.CONTAINER)) {
			return true;
		}
		if (stack.getItem() instanceof BlockItem blockItem) {
			return isInventoryBlock(blockItem.getBlock());
		}
		return false;
	}

	private static boolean isInventoryBlock(Block block) {
		return BlockEntityType.CHEST.isValid(block.defaultBlockState())
			|| BlockEntityType.TRAPPED_CHEST.isValid(block.defaultBlockState())
			|| BlockEntityType.BARREL.isValid(block.defaultBlockState())
			|| BlockEntityType.SHULKER_BOX.isValid(block.defaultBlockState())
			|| BlockEntityType.HOPPER.isValid(block.defaultBlockState())
			|| BlockEntityType.DISPENSER.isValid(block.defaultBlockState())
			|| BlockEntityType.DROPPER.isValid(block.defaultBlockState())
			|| BlockEntityType.FURNACE.isValid(block.defaultBlockState())
			|| BlockEntityType.BLAST_FURNACE.isValid(block.defaultBlockState())
			|| BlockEntityType.SMOKER.isValid(block.defaultBlockState())
			|| BlockEntityType.BREWING_STAND.isValid(block.defaultBlockState())
			|| BlockEntityType.LECTERN.isValid(block.defaultBlockState())
			|| BlockEntityType.DECORATED_POT.isValid(block.defaultBlockState());
	}
}
