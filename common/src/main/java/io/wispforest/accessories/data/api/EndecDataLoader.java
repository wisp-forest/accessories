package io.wispforest.accessories.data.api;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.mixin.SimpleJsonResourceReloadListenerAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;


// TODO: 1.21.4 ADJUSTMENTS SHOULD BE MADE TO USE LESS DIRECT CODE ANYWAYS
public abstract class EndecDataLoader<T> extends SimpleJsonResourceReloadListener<T> {

    protected final String type;

    protected final ResourceLocation id;
    protected final Endec<T> endec;

    protected final Set<ResourceLocation> dependencies;

    protected final SerializationContext context;

    protected final boolean requiresRegistries;

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType) {
        this(id, type, endec, packType, false);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, Set<ResourceLocation> value) {
        this(id, type, endec, packType, SerializationContext.empty(),false, value);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, boolean requiresRegistries) {
        this(id, type, endec, packType, SerializationContext.empty(), requiresRegistries);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, SerializationContext context, boolean requiresRegistries) {
        this(id, type, endec, packType, context, requiresRegistries, Set.of());
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, SerializationContext context, boolean requiresRegistries, Set<ResourceLocation> value) {
        super(new DelayedRecursiveCodec<>(), FileToIdConverter.json(type));

        this.id = id;
        this.type = type;
        this.endec = endec;
        this.context = context;
        this.requiresRegistries = requiresRegistries;
        this.dependencies = value;

        setupCodec();

        AccessoriesInternals.registerLoader(packType, this, (packType.equals(PackType.SERVER_DATA) ? this::setupOps : null));

        if (packType.equals(PackType.SERVER_DATA) && this instanceof SyncedDataHelper<?> syncedDataLoader) {
            SyncedDataHelperManager.registerLoader(syncedDataLoader);
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    public Set<ResourceLocation> getDependencyIds() {
        return dependencies;
    }

    protected void setupCodec() {
        ((DelayedRecursiveCodec<T>) ((SimpleJsonResourceReloadListenerAccessor<T>) this).getCodec())
                .setup(this.endec.toString(), codec -> CodecUtils.toCodec(endec, this.getContext()));
    }

    @Nullable
    private HolderLookup.Provider registries = null;

    @ApiStatus.Internal
    private EndecDataLoader<T> setupOps(HolderLookup.Provider registries) {
        this.registries = registries;

        // Resets the given converted endec to grab new context with current registries
        setupCodec();

        return this;
    }

    private SerializationContext getContext() {
        if (requiresRegistries) {
            Objects.requireNonNull(registries, "Can not build the needed context for the ManagedEndecDataLoader: " + this.getId());

            return this.context.withAttributes(RegistriesAttribute.fromInfoGetter(new RegistryOps.HolderLookupAdapter(registries)));
        }

        return this.context;
    }

    @Override
    @ApiStatus.Internal
    protected Map<ResourceLocation, T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        if (requiresRegistries && registries == null) {
            throw new IllegalStateException("Unable to prepare files as the given Registry access has not been setup on the server! [Id: " + this.getId() + "]");
        }

        var entries = super.prepare(resourceManager, profiler);

        this.registries = null;

        return entries;
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
