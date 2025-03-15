package io.wispforest.accessories.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.function.BiConsumer;


// TODO: 1.21.4 ADJUSTMENTS SHOULD BE MADE TO USE LESS DIRECT CODE ANYWAYS
public abstract class EndecDataLoader<T> extends SimpleJsonResourceReloadListener {

    protected static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    protected final String type;

    protected final ResourceLocation id;
    protected final Endec<T> endec;

    protected SerializationContext context;

    protected EndecDataLoader(SerializationContext context, ResourceLocation id, String type, Endec<T> endec) {
        super(GSON, type);

        this.type = type;

        this.id = id;

        this.context = context;
        this.endec = endec;
    }

    public static <T> EndecDataLoader<T> client(ResourceLocation id, String type, Endec<T> endec, BiConsumer<ResourceLocation, T> handleEntry) {
        return new EndecDataLoader<T>(SerializationContext.empty(), id, type, endec) {
            @Override public void handleRawEntry(ResourceLocation identifier, T t) { handleEntry.accept(identifier, t); }
        };
    }

    public static <T> EndecDataLoader<T> server(HolderLookup.Provider registries, ResourceLocation id, String type, Endec<T> endec, BiConsumer<ResourceLocation, T> handleEntry) {
        return new EndecDataLoader<T>(SerializationContext.attributes(RegistriesAttribute.of((RegistryAccess) registries)), id, type, endec) {
            @Override public void handleRawEntry(ResourceLocation identifier, T t) { handleEntry.accept(identifier, t); }
        };
    }

    protected abstract void handleRawEntry(ResourceLocation identifier, T t);

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loadedObjects, net.minecraft.server.packs.resources.ResourceManager resourceManager, ProfilerFiller profiler) {
        for (var entry : loadedObjects.entrySet()) {
            var location = entry.getKey();
            var t = this.endec.decodeFully(this.context, GsonDeserializer::of, entry.getValue());

            this.handleRawEntry(location, t);
        }
    }

    public ResourceLocation getLoaderId() {
        return id;
    }
}
