package me.srrapero720.dimthread.init;

import me.srrapero720.dimthread.DimConfig;
import net.minecraft.world.GameRules;
import me.srrapero720.dimthread.DimThread;
import me.srrapero720.dimthread.gamerule.BoolRule;
import me.srrapero720.dimthread.gamerule.IntRule;

public class ModGameRules {

	public static BoolRule ACTIVE;
	public static IntRule THREAD_COUNT;

	public static void register() {
		ACTIVE = BoolRule.builder("active", GameRules.Category.UPDATES).setInitial(true)
				.setCallback(DimThread.MANAGER::setActive).build();

		THREAD_COUNT = IntRule.builder("thread_count", GameRules.Category.UPDATES).setInitial(Math.min(DimConfig.DEFAULT_GAMERULE_THREADS.get(), Runtime.getRuntime().availableProcessors()))
				.setBounds(1, Runtime.getRuntime().availableProcessors()).setCallback(DimThread.MANAGER::setThreadCount).build();
	}

}