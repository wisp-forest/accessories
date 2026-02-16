package io.wispforest.accessories.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiConsumer;

@Mixin(GameRules.class)
public interface GameRulesAccessor {
    @Invoker("register")
    static <T extends GameRules.Value<T>> GameRules.Key<T> accessories$register(String name, GameRules.Category category, GameRules.Type<T> type) {
        throw new IllegalStateException("UHHHHHHHHH");
    }

    @Mixin(GameRules.BooleanValue.class)
    interface BooleanValueAccessor {
        @Invoker("create")
        static GameRules.Type<GameRules.BooleanValue> accessories$create(boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> changeListener) {
            throw new IllegalStateException("UHHHHHHHHH");
        }
    }
}
