package io.wispforest.accessories.pond;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface ContextedFileToIdConverter {
    <T> FileToIdConverter setData(Identifier location, T t);

    <T> @Nullable T getData(Identifier location);

    default <T> T getDataOrDefault(Identifier location, T defaultValue) {
        var data = (T) getData(location);

        if (data == null) data = defaultValue;

        return data;
    }
}
