/*
 * This file is part of Dimensional Threading Reforged - https://github.com/CCr4ft3r/DimensionalThreading-Reforged
 * Copyright (C) WearBlackAllDay and contributors: https://github.com/WearBlackAllDay/DimensionalThreading
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package wearblackallday.dimthread.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

	@Shadow @Final public static IntegerProperty POWER;
	@Shadow @Final public static Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION;

	@Shadow protected abstract BlockState getConnectionState(BlockGetter world, BlockState state, BlockPos pos);

	/**
	 * {@code RedstoneWireBlock#wiresGivePower} is not thread-safe since it's a global flag. To ensure
	 * no interference between threads, the field is replaced with this thread local one.
	 *
	 * @see RedStoneWireBlock#isSignalSource(BlockState)
	 * */
	@Unique
	private final ThreadLocal<Boolean> dimThreads$wiresGivePowerSafe = ThreadLocal.withInitial(() -> true);

	@Inject(method = "calculateTargetStrength", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I",
			shift = At.Shift.BEFORE))
	private void getReceivedRedstonePowerBefore(Level world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		this.dimThreads$wiresGivePowerSafe.set(false);
	}

	@Inject(method = "calculateTargetStrength", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I",
			shift = At.Shift.AFTER))
	private void getReceivedRedstonePowerAfter(Level world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		this.dimThreads$wiresGivePowerSafe.set(true);
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public boolean isSignalSource(BlockState state) {
		return this.dimThreads$wiresGivePowerSafe.get();
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		return !this.dimThreads$wiresGivePowerSafe.get() ? 0 : state.getSignal(world, pos, direction);
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		if(!this.dimThreads$wiresGivePowerSafe.get() || direction == Direction.DOWN) {
			return 0;
		}

		int i = state.getValue(POWER);
		if(i == 0)return 0;
		return direction != Direction.UP && !this.getConnectionState(world, state, pos)
				.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected() ? 0 : i;
	}

}