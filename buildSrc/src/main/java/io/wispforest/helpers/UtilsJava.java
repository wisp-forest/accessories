package io.wispforest.helpers;

import org.gradle.api.Transformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UtilsJava {

    public static final String removeLineTarget = "#REMOVE_LINE#";

    public static Transformer<@Nullable String, String> removeLineTransformer() {
        return new Transformer<@Nullable String, String>() {
            @Override
            public @Nullable String transform(@NotNull String value) {
                return (value.contains(removeLineTarget)) ? null : value;
            }
        };
    }
}
