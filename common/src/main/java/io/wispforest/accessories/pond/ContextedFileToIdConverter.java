package io.wispforest.accessories.pond;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface ContextedFileToIdConverter {
    <T> FileToIdConverter setData(ResourceLocation location, T t);

    <T> @Nullable T getData(ResourceLocation location);

    default <T> T getDataOrDefault(ResourceLocation location, T defaultValue) {
        var data = (T) getData(location);

        if (data == null) data = defaultValue;

        return data;
    }
}
