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

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.thread.IMutableMainThread;

@Mixin(value = ServerChunkCache.class, priority = 1001)
public abstract class ServerChunkManagerMixin extends ChunkSource implements IMutableMainThread {
	@Shadow @Final @Mutable Thread mainThread;
	@Shadow @Final public ChunkMap chunkMap;
	@Shadow @Final public ServerLevel level;

	@Override
	@Unique
	public Thread dimThreads$getMainThread() {
		return this.mainThread;
	}

	@Override
	@Unique
	public void dimThreads$setMainThread(Thread thread) {
		this.mainThread = thread;
	}

	@Inject(method = "getTickingGenerated", at = @At("HEAD"), cancellable = true)
	private void getTotalChunksLoadedCount(CallbackInfoReturnable<Integer> ci) {
		if(!FMLLoader.isProduction()) {
			int count = this.chunkMap.getTickingGenerated();
			if(count < 441)ci.setReturnValue(441);
		}
	}

	@Redirect(method = "getChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
	public Thread currentThread(int p_8360_, int p_8361_, ChunkStatus p_8362_, boolean p_8363) {
		Thread thread = Thread.currentThread();

		if(DimThread.MANAGER.isActive(this.level.getServer()) && DimThread.owns(thread)) {
			return this.mainThread;
		}

		return thread;
	}

}