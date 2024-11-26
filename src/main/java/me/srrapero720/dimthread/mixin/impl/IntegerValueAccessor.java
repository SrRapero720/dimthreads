package me.srrapero720.dimthread.mixin.impl;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRules.IntegerValue.class)
public interface IntegerValueAccessor {
	@Accessor
	int getValue();

	@Accessor
	void setValue(int value);
}