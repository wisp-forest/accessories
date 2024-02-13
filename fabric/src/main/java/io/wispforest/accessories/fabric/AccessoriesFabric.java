package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.InstanceCodecable;
import io.wispforest.accessories.api.events.extra.ImplementedEvents;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AccessoriesFabric implements ModInitializer {

    public static final AttachmentType<AccessoriesHolder> HOLDER_ATTACHMENT_TYPE;

    public static final ResourceLocation SLOT_LOADER_LOCATION = Accessories.of("slot_loader");
    public static final ResourceLocation ENTITY_SLOT_LOADER_LOCATION = Accessories.of("entity_slot_loader");
    public static final ResourceLocation SLOT_GROUP_LOADER_LOCATION = Accessories.of("slot_group_loader");

    public static final EntityApiLookup<AccessoriesCapability, Void> CAPABILITY = EntityApiLookup.get(Accessories.of("capability"), AccessoriesCapability.class, Void.class);

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolder>builder()
                .initializer(AccessoriesHolderImpl::of)
                .persistent(InstanceCodecable.constructed(AccessoriesHolderImpl::new))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("inventory_holder"));
    }

    @Override
    public void onInitialize() {
        Accessories.init();

        UseItemCallback.EVENT.register(AccessoriesEventHandler::attemptEquipFromUse);

        AccessoriesFabricNetworkHandler.INSTANCE.register();
        AccessoriesFabricNetworkHandler.INSTANCE.init();

        ServerLivingEntityEvents.AFTER_DEATH.register(AccessoriesEventHandler::onDeath);

        ServerTickEvents.START_WORLD_TICK.register(AccessoriesEventHandler::onWorldTick);

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            ImplementedEvents.clearEndermanAngryCache();
        });

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if(!joined) return;

            AccessoriesEventHandler.dataSync(null, player);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                var lookup = CAPABILITY;

                if(lookup.getProvider(entityType) != null) continue;

                lookup.registerForType((entity, unused) -> {
                    if(!(entity instanceof LivingEntity livingEntity)) return null;

                    var slots = AccessoriesAPI.getEntitySlots(livingEntity);

                    if(slots.isEmpty()) return null;

                    return new AccessoriesCapabilityImpl(livingEntity);
                }, entityType);
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if(!(entity instanceof LivingEntity livingEntity)) return;

            AccessoriesEventHandler.entityLoad(livingEntity, world);
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if(!(trackedEntity instanceof LivingEntity livingEntity)) return;

            AccessoriesEventHandler.onTracking(livingEntity, player);
        });

        var manager = ResourceManagerHelper.get(PackType.SERVER_DATA);

        manager.registerReloadListener(new IdentifiableResourceReloadListenerImpl(SLOT_LOADER_LOCATION, SlotTypeLoader.INSTANCE));
        manager.registerReloadListener(new IdentifiableResourceReloadListenerImpl(ENTITY_SLOT_LOADER_LOCATION, EntitySlotLoader.INSTANCE, SLOT_LOADER_LOCATION));
        manager.registerReloadListener(new IdentifiableResourceReloadListenerImpl(SLOT_GROUP_LOADER_LOCATION, SlotGroupLoader.INSTANCE, SLOT_LOADER_LOCATION));

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> AccessoriesEventHandler.dataReloadOccured = true);
    }

    private record IdentifiableResourceReloadListenerImpl(ResourceLocation location, PreparableReloadListener listener, ResourceLocation ...dependencies) implements IdentifiableResourceReloadListener {
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
            return new HashSet<>(Set.of(dependencies));
        }
    }

}