package me.srrapero720.dimthread.gamerule;

import net.minecraft.world.GameRules;
import me.srrapero720.dimthread.DimThread;

public abstract class GameRule<T extends GameRules.RuleValue<T>> {


	private final GameRules.RuleKey<T> key;
	private final GameRules.RuleType<T> rule;

	public GameRule(String name, GameRules.Category category, GameRules.RuleType<T> rule) {
		this.key = GameRules.register(DimThread.MOD_ID + "_" + name, category, rule);
		this.rule = rule;
	}

	public GameRules.RuleKey<T> getKey() {
		return this.key;
	}

	@SuppressWarnings("unused")
	public GameRules.RuleType<T> getRule() {
		return this.rule;
	}

}