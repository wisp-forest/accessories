package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.arguments.ArgumentType;
import io.wispforest.accessories.commands.api.base.Argument;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class Arguments {

    public static <T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter) {
        return Argument.required(name, type, getter);
    }

    public static <T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter, T defaultValue) {
        return Argument.defaulted(name, type, getter, defaultValue);
    }

    public static Argument<String> branches(String ...branches) {
        return Argument.branches(List.of(branches));
    }

    public static Argument<String> branches(List<String> branches) {
        return Argument.branches(branches);
    }
}
