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
package wearblackallday.dimthread;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import wearblackallday.dimthread.init.ModGameRules;
import wearblackallday.dimthread.thread.IMutableMainThread;
import wearblackallday.dimthread.util.ServerManager;
import wearblackallday.util.ThreadPool;

@Mod(DimThread.MOD_ID)
public class DimThread {

    public static final String MOD_ID = "dimthread";
    public static final ServerManager MANAGER = new ServerManager();

    public DimThread() {
        ModGameRules.registerGameRules();
    }

    public static ThreadPool getThreadPool(MinecraftServer server) {
        return MANAGER.getThreadPool(server);
    }

    public static boolean isModPresent(String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
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
        thread.setName(MOD_ID + "_" + name);
    }

    public static void attach(Thread thread, ServerLevel world) {
        attach(thread, world.dimension().location().getPath());
    }

    /**
     * Checks if the given thread is a dimthread worker by checking the name. Probably quite fragile...
     */
    public static boolean owns(Thread thread) {
        return thread.getName().startsWith(MOD_ID);
    }
}