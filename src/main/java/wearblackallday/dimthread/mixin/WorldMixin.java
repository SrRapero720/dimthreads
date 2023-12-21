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

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import wearblackallday.dimthread.thread.IMutableMainThread;

@Mixin(Level.class)
public abstract class WorldMixin implements IMutableMainThread {

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