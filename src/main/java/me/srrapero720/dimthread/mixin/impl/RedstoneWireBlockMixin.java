package me.srrapero720.dimthread.mixin.impl;

import me.srrapero720.dimthread.mixin.tools.ThreadLocal;
import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.*;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {
	@Shadow @ThreadLocal private boolean shouldSignal;
}