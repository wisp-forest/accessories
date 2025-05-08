package io.wispforest.accessories.data.api;

import io.wispforest.endec.Endec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.Map;

public class SimpleManagedEndecDataLoader<T> extends ManagedEndecDataLoader<T, T> {

    protected SimpleManagedEndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType) {
        super(id, type, endec, endec, packType);
    }

    @Override
    public Map<ResourceLocation, T> mapFrom(Map<ResourceLocation, T> rawData) {
        return rawData;
    }
}
