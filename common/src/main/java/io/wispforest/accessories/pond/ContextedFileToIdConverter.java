package io.wispforest.accessories.pond;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;

public interface ContextedFileToIdConverter {
    <T> FileToIdConverter setData(ResourceLocation location, T t);

    <T> T getData(ResourceLocation location);
}
