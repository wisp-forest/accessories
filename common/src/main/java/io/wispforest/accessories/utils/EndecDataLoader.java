package io.wispforest.accessories.utils;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.accessories.mixin.SimpleJsonResourceReloadListenerAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;


// TODO: 1.21.4 ADJUSTMENTS SHOULD BE MADE TO USE LESS DIRECT CODE ANYWAYS
public abstract class EndecDataLoader<T> extends SimpleJsonResourceReloadListener<T> {

    protected final String type;

    protected final ResourceLocation id;
    protected final Endec<T> endec;

    protected final SerializationContext context;

    protected EndecDataLoader(SerializationContext context, ResourceLocation id, String type, Endec<T> endec) {
        super(new DelayedRecursiveCodec<>(), FileToIdConverter.json(type));

        this.type = type;

        this.id = id;

        this.context = context;
        this.endec = endec;

        setupCodec();
    }

    protected void setupCodec() {
        ((DelayedRecursiveCodec<T>) ((SimpleJsonResourceReloadListenerAccessor<T>) this).getCodec())
                .setup(this.endec.toString(), codec -> CodecUtils.toCodec(endec, this.getContext()));
    }

    protected SerializationContext getContext() {
        return this.context;
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
    protected void apply(Map<ResourceLocation, T> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        object.forEach(this::handleRawEntry);
    }

    public ResourceLocation getLoaderId() {
        return id;
    }

    private static class DelayedRecursiveCodec<T> implements Codec<T> {
        private String name;
        private Supplier<Codec<T>> wrapped;

        public void setup(String name, Function<Codec<T>, Codec<T>> wrapped) {
            this.name = name;
            this.wrapped = Suppliers.memoize(() -> wrapped.apply(this));
        }

        @Override
        public <S> DataResult<Pair<T, S>> decode(final DynamicOps<S> ops, final S input) {
            return wrapped.get().decode(ops, input);
        }

        @Override
        public <S> DataResult<S> encode(final T input, final DynamicOps<S> ops, final S prefix) {
            return wrapped.get().encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "RecursiveCodec[" + name + ']';
        }
    }
}
