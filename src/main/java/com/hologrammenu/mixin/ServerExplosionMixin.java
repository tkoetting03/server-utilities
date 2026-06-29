package com.hologrammenu.mixin;

import com.hologrammenu.storage.StorageMenuBlockStore;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@ModifyVariable(
		method = "explode",
		at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"),
		ordinal = 0
	)
	private List<BlockPos> hologrammenu$filterInvulnerableBlocks(List<BlockPos> toBlow) {
		toBlow.removeIf(pos -> StorageMenuBlockStore.isInvulnerable(this.level, pos));
		return toBlow;
	}
}
