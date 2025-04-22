package io.wispforest.accessories.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.mixin.ConfigurableRegistryLookupAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.network.ClientAccess;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ManagedEndecDataLoader<T> extends EndecDataLoader<T>  {

    private static final Map<ResourceLocation, ManagedEndecDataLoader<?>> ALL_DATA_LOADERS = new LinkedHashMap<>();

    private final BiMap<ResourceLocation, T> server = HashBiMap.create();
    private final BiMap<ResourceLocation, T> client = HashBiMap.create();

    private final Endec<BiMap<ResourceLocation, T>> mapEndec;

    private BiConsumer<ResourceLocation, T> handleEntry = (location, t) -> {};

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<T> endec) {
        super(SerializationContext.empty(), id, type, endec);

        this.mapEndec = biMapEndec(ResourceLocation::toString, ResourceLocation::tryParse, endec);

        ALL_DATA_LOADERS.put(id, this);

        AccessoriesInternals.registerLoader(this, this::setupOps);
    }

    public static <T> ManagedEndecDataLoader<T> of(ResourceLocation id, String type, Endec<T> endec) {
        return of(id, type, endec);
    }

    public ManagedEndecDataLoader<T> onEntryAdd(BiConsumer<ResourceLocation, T> value) {
        this.handleEntry = value;

        return this;
    }

    @Nullable
    public static <T> ManagedEndecDataLoader<T> getLoader(ResourceLocation id) {
        return (ManagedEndecDataLoader<T>) ALL_DATA_LOADERS.get(id);
    }

    @Override
    protected void handleRawEntry(ResourceLocation id, T t) {
        handleEntry.accept(id, t);

        server.put(id, t);
    }

    public Endec<BiMap<ResourceLocation, T>> mapEndec() {
        return this.mapEndec;
    }

    private void handleUnsafeSync(BiMap<ResourceLocation, ?> map) {
        this.client.clear();
        this.client.putAll((Map<? extends ResourceLocation, ? extends T>) map);

        this.onSync();
    }

    protected void onSync() {}

    public Map<ResourceLocation, T> getEntries(Level level) {
        return getEntries(level.isClientSide());
    }

    public Map<ResourceLocation, T> getEntries(boolean isClientSide) {
        return Collections.unmodifiableMap(isClientSide ? client : server);
    }

    @Nullable
    public T getEntry(ResourceLocation id, Level level) {
        return getEntry(id, level.isClientSide());
    }

    @Nullable
    public T getEntry(ResourceLocation id, boolean isClientSide) {
        return (isClientSide ? client : server).get(id);
    }

    public ResourceLocation getId(T t, Level level) {
        return getId(t, level.isClientSide());
    }

    public ResourceLocation getId(T t, boolean isClientSide) {
        return (isClientSide ? client : server).inverse().get(t);
    }

    //--


    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loadedObjects, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.server.clear();

        super.apply(loadedObjects, resourceManager, profiler);
    }

    @ApiStatus.Internal
    private ManagedEndecDataLoader<T> setupOps(HolderLookup.Provider registries) {
        if (registries instanceof ReloadableServerResources.ConfigurableRegistryLookup lookup) {
            this.context = context.withAttributes(RegistriesAttribute.of(((ConfigurableRegistryLookupAccessor) lookup).getRegistryAccess()));
        } else {
            this.context = context.withAttributes(RegistriesAttribute.of((RegistryAccess) registries));
        }

        return this;
    }

    @Override
    @ApiStatus.Internal
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        if (!this.context.hasAttribute(RegistriesAttribute.REGISTRIES)) {
            throw new IllegalStateException("Unable to prepare files as the given Registry access has not been setup on the server! [Id: " + this.getLoaderId() + "]");
        }

        return super.prepare(resourceManager, profiler);
    }

    @ApiStatus.Internal
    private static <K, V> Endec<BiMap<K, V>> biMapEndec(Function<K, String> keyToString, Function<String, K> stringToKey, Endec<V> valueEndec) {
        return Endec.of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, valueEndec, map.size())) {
                map.forEach((k, v) -> mapState.entry(keyToString.apply(k), v));
            }
        }, (ctx, deserializer) -> {
            var mapState = deserializer.map(ctx, valueEndec);

            var map = HashBiMap.<K, V>create(mapState.estimatedSize());
            mapState.forEachRemaining(entry -> map.put(stringToKey.apply(entry.getKey()), entry.getValue()));

            return map;
        });
    }

    @ApiStatus.Internal
    public static void init(OwoNetChannel channel, Consumer<Consumer<Player>> hookRegistration) {
        channel.registerClientboundDeferred(SyncAllLoaderDataPacket.class, SyncAllLoaderDataPacket.ENDEC);

        hookRegistration.accept(player -> {
            var packets = ALL_DATA_LOADERS.values().stream()
                    .map(dataLoader -> new SyncLoaderDataPacket(dataLoader.id, (BiMap<ResourceLocation, Object>) dataLoader.server))
                    .toList();

            channel.serverHandle(player).send(new SyncAllLoaderDataPacket(packets));
        });
    }

    @ApiStatus.Internal
    public static void initClient(OwoNetChannel channel) {
        channel.registerClientbound(SyncAllLoaderDataPacket.class, SyncAllLoaderDataPacket.ENDEC, SyncAllLoaderDataPacket::handle);
    }

    @ApiStatus.Internal
    private record SyncAllLoaderDataPacket(List<SyncLoaderDataPacket> packets) {
        private static final StructEndec<SyncAllLoaderDataPacket> ENDEC = StructEndecBuilder.of(
                SyncLoaderDataPacket.ENDEC.listOf().fieldOf("packets", SyncAllLoaderDataPacket::packets),
                SyncAllLoaderDataPacket::new
        );

        private static void handle(SyncAllLoaderDataPacket packet, ClientAccess access) {
            for (var innerPacket : packet.packets()) {
                SyncLoaderDataPacket.handle(innerPacket, access);
            }
        }
    }

    @ApiStatus.Internal
    private record SyncLoaderDataPacket(ResourceLocation id, BiMap<ResourceLocation, Object> data) {
        private static final Map<ResourceLocation, StructEndec<SyncLoaderDataPacket>> CACHED_ENDECS = new HashMap<>();

        private static final StructEndec<SyncLoaderDataPacket> ENDEC = (StructEndec<SyncLoaderDataPacket>) Endec.dispatched(
                id -> {
                    var loader = getLoader(id);

                    if (loader == null) {
                        throw new IllegalStateException("Unable to get following Data Loader to handle the given sync packet: " + id);
                    }

                    return CACHED_ENDECS.computeIfAbsent(id, identifier -> {
                        return StructEndecBuilder.of(
                                MinecraftEndecs.IDENTIFIER.fieldOf("id", SyncLoaderDataPacket::id),
                                loader.mapEndec.fieldOf("data", SyncLoaderDataPacket::data),
                                SyncLoaderDataPacket::new
                        );
                    });
                },
                SyncLoaderDataPacket::id,
                MinecraftEndecs.IDENTIFIER);

        private static void handle(SyncLoaderDataPacket packet, ClientAccess access) {
            getLoader(packet.id()).handleUnsafeSync(packet.data());
        }
    }
}
