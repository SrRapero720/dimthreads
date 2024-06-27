package me.srrapero720.dimthread.mixin;

import me.srrapero720.dimthread.thread.IMutableMainThread;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface ILevelMixin extends IMutableMainThread {
    @Accessor("thread")
    void dimThreads$setMainThread(Thread t);

    @Accessor("thread")
    Thread dimThreads$getMainThread();
}
