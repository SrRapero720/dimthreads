package me.srrapero720.dimthread.gamerule;

import com.mojang.logging.LogUtils;
import me.srrapero720.dimthread.mixin.impl.IntegerValueAccessor;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class RestrictedIntegerValue extends GameRules.IntegerValue {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final int min;
    private final int max;

    public RestrictedIntegerValue(GameRules.Type<GameRules.IntegerValue> type, int def, int min, int max) {
        super(type, def);
        this.min = min;
        this.max = max;
    }

    @Override
    protected void deserialize(@NotNull String value) {
        final int i = safeParse(value);

        if (i < this.min || i > this.max) {
            return;
        }
        ((IntegerValueAccessor) this).setValue(i);
    }

    @Override
    public boolean tryDeserialize(@NotNull String input) {
        try {
            int value = Integer.parseInt(input);

            if (this.min > value || this.max < value) {
                return false;
            }

            ((IntegerValueAccessor) this).setValue(value);
            return true;
        } catch (NumberFormatException var3) {
            return false;
        }
    }

    @Override
    protected GameRules.@NotNull IntegerValue copy() {
        return new RestrictedIntegerValue(this.type, ((IntegerValueAccessor) this).getValue(), this.min, this.max);
    }

    private static int safeParse(String input) {
        if (!input.isEmpty()) {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException var2) {
                LOGGER.warn("Failed to parse integer {}", input);
            }
        }

        return 0;
    }
}