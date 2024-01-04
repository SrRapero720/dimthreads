package me.srrapero720.dimthread.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.RedstoneSide;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

	@Shadow @Final public static IntegerProperty POWER;
	@Shadow @Final public static Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION;

	@Shadow protected abstract BlockState getConnectionState(IBlockReader world, BlockState state, BlockPos pos);

	/**
	 * {@code RedstoneWireBlock#wiresGivePower} is not thread-safe since it's a global flag. To ensure
	 * no interference between threads, the field is replaced with this thread local one.
	 *
	 * @see RedstoneWireBlock#isSignalSource(BlockState)
	 * */
	@Unique
	private final ThreadLocal<Boolean> dimThreads$wiresGivePowerSafe = ThreadLocal.withInitial(() -> true);

	@Inject(method = "calculateTargetStrength", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;getBestNeighborSignal(Lnet/minecraft/util/math/BlockPos;)I",
			shift = At.Shift.BEFORE))
	private void getReceivedRedstonePowerBefore(World world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
		this.dimThreads$wiresGivePowerSafe.set(false);
	}

	@Inject(method = "calculateTargetStrength", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;getBestNeighborSignal(Lnet/minecraft/util/math/BlockPos;)I",
			shift = At.Shift.AFTER))
	private void getReceivedRedstonePowerAfter(World world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
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
	public int getDirectSignal(BlockState state, IBlockReader world, BlockPos pos, Direction direction) {
		return !this.dimThreads$wiresGivePowerSafe.get() ? 0 : state.getSignal(world, pos, direction);
	}

	/**
	 * @author DimensionalThreading (WearBlackAllDay)
	 * @reason Made redstone thread-safe, please inject in the caller.
	 */
	@Overwrite
	public int getSignal(BlockState state, IBlockReader world, BlockPos pos, Direction direction) {
		if(!this.dimThreads$wiresGivePowerSafe.get() || direction == Direction.DOWN) {
			return 0;
		}

		int i = state.getValue(POWER);
		if(i == 0)return 0;
		return direction != Direction.UP && !this.getConnectionState(world, state, pos)
				.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected() ? 0 : i;
	}

}