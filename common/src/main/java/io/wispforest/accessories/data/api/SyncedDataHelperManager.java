package io.wispforest.accessories.data.api;

import com.mojang.logging.LogUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.network.ClientAccess;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SyncedDataHelperManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, SyncedDataHelper<?>> ALL_SYNCED_LOADERS = new LinkedHashMap<>();

    public static void registerLoader(SyncedDataHelper<?> loader) {
        if (ALL_SYNCED_LOADERS.containsKey(loader.getId())) {
            throw new IllegalStateException("An already existing SyncedDataLoader has been registered! [Id: " + loader.getId() + "]");
        }

        ALL_SYNCED_LOADERS.put(loader.getId(), loader);
    }

    @Nullable
    public static SyncedDataHelper<?> getLoader(ResourceLocation id) {
        return ALL_SYNCED_LOADERS.get(id);
    }

    @ApiStatus.Internal
    public static void init(OwoNetChannel channel, Consumer<Consumer<Player>> hookRegistration) {
        channel.registerClientboundDeferred(SyncAllLoaderDataPacket.class, SyncAllLoaderDataPacket.ENDEC);

        hookRegistration.accept(player -> {
            var endecDataLoaders = ALL_SYNCED_LOADERS.values()
                    .stream()
                    .collect(Collectors.toList());

            Set<ResourceLocation> resolvedIds = new HashSet<>();

            for (SyncedDataHelper<?> dataLoader : endecDataLoaders) {
                resolvedIds.add(dataLoader.getId());
            }

            List<SyncedDataHelper<?>> dataLoaders = new ArrayList<>();

            int lastSize = -1;

            while (dataLoaders.size() != lastSize) {
                lastSize = dataLoaders.size();

                var it = endecDataLoaders.iterator();

                while (it.hasNext()) {
                    SyncedDataHelper<?> dataLoader = it.next();

                    if (resolvedIds.containsAll(dataLoader.getDependencyIds())) {
                        resolvedIds.add(dataLoader.getId());
                        dataLoaders.add(dataLoader);
                        it.remove();
                    }
                }
            }

            var packets = dataLoaders.stream()
                    .map(dataLoader -> {
                        var id = dataLoader.getId();
                        var data = dataLoader.getServerData();

                        return new SyncLoaderDataPacket(id, data);
                    })
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
    private static final class SyncLoaderDataPacket {
        private static final Map<ResourceLocation, StructEndec<SyncLoaderDataPacket>> CACHED_ENDECS = new HashMap<>();

        private static final StructEndec<SyncLoaderDataPacket> ENDEC = Endec.dispatched(
                id -> {
                    var loader = getLoader(id);

                    if (loader == null) {
                        throw new IllegalStateException("Unable to get following Data Loader to handle the given sync packet: " + id);
                    }

                    return CACHED_ENDECS.computeIfAbsent(id, identifier -> {
                        return StructEndecBuilder.of(
                                MinecraftEndecs.IDENTIFIER.fieldOf("id", SyncLoaderDataPacket::id),
                                ((Endec<Object>) loader.syncDataEndec()).fieldOf("data", SyncLoaderDataPacket::data),
                                SyncLoaderDataPacket::new
                        );
                    });
                },
                SyncLoaderDataPacket::id,
                MinecraftEndecs.IDENTIFIER);

        private final ResourceLocation id;
        private final Object data;

        private SyncLoaderDataPacket(ResourceLocation id, Object data) {
            this.id = id;
            this.data = data;
        }

        private static void handle(SyncLoaderDataPacket packet, ClientAccess access) {
            var loader = getLoader(packet.id());

            var exception = loader.onReceivedDataUnsafe(packet.data());

            if (exception != null) {
                LOGGER.error("An error has occured when attempting to send sync data to the given SyncedDataLoader: {}", packet.id(), exception);

                throw new RuntimeException(exception);
            }
        }

        public ResourceLocation id() {
            return id;
        }

        public Object data() {
            return data;
        }
    }
}
