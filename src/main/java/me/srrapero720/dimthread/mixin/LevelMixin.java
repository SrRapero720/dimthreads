package me.srrapero720.dimthread.mixin;

import me.srrapero720.dimthread.thread.IMutableMainThread;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;

@Mixin(Level.class)
public abstract class LevelMixin implements IMutableMainThread {

	@Shadow @Final @Mutable private Thread thread;

	@Override
	@Unique
	public Thread dimThreads$getMainThread() {
		return this.thread;
	}

	@Override
	@Unique
	public void dimThreads$setMainThread(Thread thread) {
		this.thread = thread;
	}

}