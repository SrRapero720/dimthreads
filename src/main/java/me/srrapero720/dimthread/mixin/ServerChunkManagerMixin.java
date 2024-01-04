package me.srrapero720.dimthread.mixin;

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
import me.srrapero720.dimthread.DimThread;
import me.srrapero720.dimthread.thread.IMutableMainThread;

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