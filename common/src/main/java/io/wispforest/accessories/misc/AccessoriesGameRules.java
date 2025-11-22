package io.wispforest.accessories.misc;

import io.wispforest.accessories.mixin.GameRulesAccessor;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

import static net.minecraft.world.level.GameRules.*;

@ApiStatus.Internal
public class AccessoriesGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> RULE_KEEP_ACCESSORY_INVENTORY = register("keepAccessoryInventory", Category.PLAYER, createBooleanRuleType(false));

    public static <T extends Value<T>> Key<T> register(String name, Category category, Type<T> type) {
        return GameRulesAccessor.accessories$register("accessories." + name, category, type);
    }

    public static GameRules.Type<GameRules.BooleanValue> createBooleanRuleType(boolean defaultValue) {
        return createBooleanRuleType(defaultValue, (booleanValue) -> {});
    }

    public static GameRules.Type<GameRules.BooleanValue> createBooleanRuleType(boolean defaultValue, Consumer<GameRules.BooleanValue> consumer) {
        return GameRulesAccessor.BooleanValueAccessor.accessories$create(defaultValue, (server, booleanValue) -> consumer.accept(booleanValue));
    }
}
