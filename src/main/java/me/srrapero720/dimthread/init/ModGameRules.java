package me.srrapero720.dimthread.init;

import me.srrapero720.dimthread.DimConfig;
import me.srrapero720.dimthread.gamerule.BoolRule;
import me.srrapero720.dimthread.gamerule.IntRule;
import net.minecraft.world.level.GameRules;
import me.srrapero720.dimthread.DimThread;

public class ModGameRules {

	public static BoolRule ACTIVE;
	public static IntRule THREAD_COUNT;

	public static void register() {
		ACTIVE = BoolRule.builder("active", GameRules.Category.UPDATES).setInitial(true)
				.setCallback(DimThread.MANAGER::setActive).build();

		THREAD_COUNT = IntRule.builder("thread_count", GameRules.Category.UPDATES).setInitial(Math.min(DimThread.CONFIG.DEFAULT_GAMERULE_THREADS, Runtime.getRuntime().availableProcessors()))
				.setBounds(1, Runtime.getRuntime().availableProcessors()).setCallback(DimThread.MANAGER::setThreadCount).build();
	}

}