package me.srrapero720.dimthread.gamerule;

import net.minecraft.world.level.GameRules;
import me.srrapero720.dimthread.DimThread;

public abstract class GameRule<T extends GameRules.Value<T>> {

	private final GameRules.Key<T> key;
	private final GameRules.Type<T> rule;

	public GameRule(String name, GameRules.Category category, GameRules.Type<T> rule) {
		this.key = GameRules.register(DimThread.MOD_ID + "_" + name, category, rule);
		this.rule = rule;
	}

	public GameRules.Key<T> getKey() {
		return this.key;
	}

	@SuppressWarnings("unused")
	public GameRules.Type<T> getRule() {
		return this.rule;
	}

}