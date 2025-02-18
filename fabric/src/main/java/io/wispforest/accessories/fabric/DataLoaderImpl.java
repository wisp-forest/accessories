package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.DataLoaderBase;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataLoaderImpl extends DataLoaderBase {

    private IdentifiableResourceReloadListener identifiedSlotLoader = null;
    private IdentifiableResourceReloadListener identifiedEntitySlotLoader = null;

    @Override
    protected Optional<PreparableReloadListener> getIdentifiedSlotLoader() {
        return Optional.ofNullable(identifiedSlotLoader);
    }

    @Override
    protected Optional<PreparableReloadListener> getIdentifiedEntitySlotLoader() {
        return Optional.ofNullable(identifiedEntitySlotLoader);
    }

    @Override
    public void registerListeners() {
        var manager = ResourceManagerHelper.get(PackType.SERVER_DATA);

        this.identifiedSlotLoader = new IdentifiableResourceReloadListenerImpl(Accessories.SLOT_LOADER_LOCATION, SlotTypeLoader.INSTANCE);
        this.identifiedEntitySlotLoader = new IdentifiableResourceReloadListenerImpl(Accessories.ENTITY_SLOT_LOADER_LOCATION, EntitySlotLoader.INSTANCE, Accessories.SLOT_LOADER_LOCATION);

        manager.registerReloadListener(identifiedSlotLoader);
        manager.registerReloadListener(identifiedEntitySlotLoader);
        manager.registerReloadListener(new IdentifiableResourceReloadListenerImpl(Accessories.SLOT_GROUP_LOADER_LOCATION, SlotGroupLoader.INSTANCE, Accessories.SLOT_LOADER_LOCATION));

        manager.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return Accessories.DATA_RELOAD_HOOK;
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                AccessoriesEventHandler.dataReloadOccurred = true;
            }
        });

        super.registerListeners();
    }

    private record IdentifiableResourceReloadListenerImpl(ResourceLocation location, PreparableReloadListener listener, Set<ResourceLocation> dependencies) implements IdentifiableResourceReloadListener {

        public IdentifiableResourceReloadListenerImpl(ResourceLocation location, PreparableReloadListener listener, ResourceLocation ...dependencies){
            this(location, listener, new HashSet<>(List.of(dependencies)));
        }

        @Override
        public ResourceLocation getFabricId() {
            return this.location;
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2) {
            return this.listener.reload(preparationBarrier, resourceManager, executor, executor2);
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return dependencies;
        }
    }
}
