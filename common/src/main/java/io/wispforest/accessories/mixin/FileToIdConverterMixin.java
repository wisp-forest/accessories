package io.wispforest.accessories.mixin;

import io.wispforest.accessories.pond.ContextedFileToIdConverter;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(FileToIdConverter.class)
public abstract class FileToIdConverterMixin implements ContextedFileToIdConverter {

    @Unique
    private final Map<ResourceLocation, Object> contextData = new HashMap<>();

    @Override
    public <T> FileToIdConverter setData(ResourceLocation location, T t) {
        contextData.put(location, t);

        return (FileToIdConverter) (Object) this;
    }

    @Override
    public <T> T getData(ResourceLocation location) {
        return (T) contextData.get(location);
    }
}
