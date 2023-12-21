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
package wearblackallday.dimthread.util;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import wearblackallday.dimthread.init.ModGameRules;
import wearblackallday.util.ThreadPool;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;

public class ServerManager {

	private final Map<MinecraftServer, Boolean> actives = Collections.synchronizedMap(new Object2BooleanArrayMap<>());
	public final Map<MinecraftServer, ThreadPool> threadPools = Collections.synchronizedMap(new WeakHashMap<>());

	public boolean isActive(MinecraftServer server) {
		return this.actives.computeIfAbsent(server, s -> s.getGameRules().getBoolean(ModGameRules.ACTIVE.getKey()));
	}

	public void setActive(MinecraftServer server, GameRules.BooleanValue value) {
		this.actives.put(server, value.get());
	}

	public ThreadPool getThreadPool(MinecraftServer server) {
		return this.threadPools.computeIfAbsent(server, s -> new ThreadPool(s.getGameRules().getInt(ModGameRules.THREAD_COUNT.getKey())));
	}

	public void setThreadCount(MinecraftServer server, GameRules.IntegerValue value) {
		ThreadPool current = getThreadPool(server);

		if (current.getActiveCount() != 0) {
			throw new ConcurrentModificationException("Setting the thread count in wrong phase");
		}

		this.threadPools.put(server, new ThreadPool(value.get()));
		current.shutdown();
	}
}