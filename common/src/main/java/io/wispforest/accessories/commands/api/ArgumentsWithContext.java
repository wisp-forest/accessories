package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.wispforest.accessories.commands.api.base.Argument;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface ArgumentsWithContext<S> {

//    default <T, A extends ArgumentType<?> & TypedNamedArgumentGetter<S>> Argument<T> required(String name, A type) {
//        return required(name, type, type.<T>toArgumentGetter(type));
//    }
//
//    default <T, A extends ArgumentType<?> & TypedNamedArgumentGetter<S>> Argument<T> defaulted(String name, A type, T defaultValue) {
//        return defaulted(name, type, type.toArgumentGetter(type), defaultValue);
//    }
//
//    default <T, A extends ArgumentType<?> & TypedNamedArgumentGetter<S>> Argument<T> required(String name, A type, @Nullable SuggestionProvider<?> suggestions) {
//        return required(name, type, type.<T>toArgumentGetter(type), suggestions);
//    }
//
//    default <T, A extends ArgumentType<?> & TypedNamedArgumentGetter<S>> Argument<T> defaulted(String name, A type, T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
//        return defaulted(name, type, type.toArgumentGetter(type), defaultValue, suggestions);
//    }

    //--

    default <T> NamedArgumentGetter<S, T> getArgumentGetter(ArgumentType<T> type) {
        if (type instanceof NamedArgumentGetter<?, ?> getter) return (NamedArgumentGetter<S, T>) getter;

        throw new IllegalArgumentException("Unable to get needed getter for given argument type as it has no getter bound to it! [Type: " + type + "]");
    }

    default <T> Argument<T> required(String name, ArgumentType<T> type) {
        return required(name, type, getArgumentGetter(type));
    }

    default <T> Argument<T> defaulted(String name, ArgumentType<T> type, T defaultValue) {
        return defaulted(name, type, getArgumentGetter(type), defaultValue);
    }

    //--

    default <T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter) {
        return Argument.required(name, type, getter, null);
    }

    default <T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, T defaultValue) {
        return Argument.defaulted(name, type, getter, defaultValue, null);
    }

    default <T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, @Nullable SuggestionProvider<?> suggestions) {
        return Argument.required(name, type, getter, suggestions);
    }

    default <T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
        return Argument.defaulted(name, type, getter, defaultValue, suggestions);
    }

    default Argument<String> branches(String ...branches) {
        return Argument.branches(List.of(branches));
    }

    default Argument<String> branches(List<String> branches) {
        return Argument.branches(branches);
    }

    default <T> Argument<T> branches(List<String> branches, Function<String, T> conversionFunc) {
        return Argument.branches(branches, conversionFunc);
    }
}
