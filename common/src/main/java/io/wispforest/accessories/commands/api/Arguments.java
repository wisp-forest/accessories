package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.wispforest.accessories.commands.api.base.Argument;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class Arguments {

    public static <S, T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter) {
        return Argument.required(name, type, getter, null);
    }

    public static <S, T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter, T defaultValue) {
        return Argument.defaulted(name, type, getter, defaultValue, null);
    }

    public static <S, T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter, @Nullable SuggestionProvider<?> suggestions) {
        return Argument.required(name, type, getter, suggestions);
    }

    public static <S, T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<CommandSourceStack, T> getter, T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
        return Argument.defaulted(name, type, getter, defaultValue, suggestions);
    }

    public static Argument<String> branches(String ...branches) {
        return Argument.branches(List.of(branches));
    }

    public static Argument<String> branches(List<String> branches) {
        return Argument.branches(branches);
    }

    public static <T> Argument<T> branches(List<String> branches, Function<String, T> conversionFunc) {
        return Argument.branches(branches, conversionFunc);
    }
}
