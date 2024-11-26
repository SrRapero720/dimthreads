package me.srrapero720.dimthread;

import me.srrapero720.dimthread.init.ModGameRules;
import me.srrapero720.dimthread.thread.ThreadPool;
import me.srrapero720.dimthread.util.ServerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import me.srrapero720.dimthread.thread.IMutableMainThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DimThread implements ModInitializer {
    public static final String MOD_ID = "dimthread";
    public static final ServerManager MANAGER = new ServerManager();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static DimConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = new DimConfig(true);
        ModGameRules.register();
    }

    public static ThreadPool getThreadPool(MinecraftServer server) {
        return MANAGER.getThreadPool(server);
    }

    public static boolean isModPresent(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    public static synchronized void swapThreadsAndRun(Runnable task, Object... threadedObjects) {
        Thread currentThread = Thread.currentThread();
        Thread[] oldThreads = new Thread[threadedObjects.length];

        for (int i = 0; i < oldThreads.length; i++) {
            oldThreads[i] = ((IMutableMainThread) threadedObjects[i]).dimThreads$getMainThread();
            ((IMutableMainThread) threadedObjects[i]).dimThreads$setMainThread(currentThread);
        }

        task.run();

        for (int i = 0; i < oldThreads.length; i++) {
            ((IMutableMainThread) threadedObjects[i]).dimThreads$setMainThread(oldThreads[i]);
        }
    }

    /**
     * Makes it easy to understand what is happening in crash reports and helps identify dimthread workers.
     */
    public static void attach(Thread thread, String name) {
        thread.setName(MOD_ID + "_server_" + name);
    }

    public static void attach(Thread thread, ServerLevel world) {
        attach(thread, world.dimension().location().getPath());
    }

    /**
     * Checks if the given thread is a dimthread worker by checking the name. Probably quite fragile...
     */
    public static boolean owns(Thread thread) {
        return thread.getName().startsWith(MOD_ID + "_server_");
    }
}