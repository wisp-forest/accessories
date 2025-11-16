package io.wispforest.helpers;

import org.gradle.api.Transformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UtilsJava {
    public static Transformer<@Nullable String, String> removeLineTransformer() {
        return new Transformer<@Nullable String, String>() {
            @Override
            public @Nullable String transform(@NotNull String value) {
                if ((value.contains(Objects.toString(ResourceProcessingUtils.REMOVE_LINE_TARGET_START))) ||
                    (value.contains(Objects.toString(ResourceProcessingUtils.REMOVE_LINE_TARGET_END)))) {
                    return null;
                }

                return value;
            }
        };
    }
}
