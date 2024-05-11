package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.DataLoaderImplBase;
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
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataLoaderImpl extends DataLoaderImplBase {

    public static final ResourceLocation SLOT_LOADER_LOCATION = Accessories.of("slot_loader");
    public static final ResourceLocation ENTITY_SLOT_LOADER_LOCATION = Accessories.of("entity_slot_loader");
    public static final ResourceLocation SLOT_GROUP_LOADER_LOCATION = Accessories.of("slot_group_loader");

    public void registerListeners() {
        super.registerListeners();

        var manager = ResourceManagerHelper.get(PackType.SERVER_DATA);

//        FabricLoader.getInstance().getEntrypoints("data_loading_modifications", DataLoadingModifications.class)
//                .forEach(dataLoadingModifications -> {
//                    dataLoadingModifications.beforeRegistration(preparableReloadListener -> {
//                        if(preparableReloadListener instanceof IdentifiableResourceReloadListener identifiableResourceReloadListener){
//                            manager.registerReloadListener(identifiableResourceReloadListener);
//                        }
//                    });
//                });

        var SLOT_TYPE_LOADER = (IdentifiableResourceReloadListener) new IdentifiableResourceReloadListenerImpl(SLOT_LOADER_LOCATION, SlotTypeLoader.INSTANCE);

        var ENTITY_SLOT_LOADER = (IdentifiableResourceReloadListener) new IdentifiableResourceReloadListenerImpl(ENTITY_SLOT_LOADER_LOCATION, EntitySlotLoader.INSTANCE, SLOT_LOADER_LOCATION);

        manager.registerReloadListener(SLOT_TYPE_LOADER);
        manager.registerReloadListener(ENTITY_SLOT_LOADER);
        manager.registerReloadListener(new IdentifiableResourceReloadListenerImpl(SLOT_GROUP_LOADER_LOCATION, SlotGroupLoader.INSTANCE, SLOT_LOADER_LOCATION));

        manager.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return Accessories.of("data_reload_hook");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                AccessoriesEventHandler.dataReloadOccured = true;
            }
        });
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
        public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return this.listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return dependencies;
        }
    }
}
