package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.DataLoaderBase;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.InstanceCodecable;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class AccessoriesFabric implements ModInitializer {

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE;

    public static final EntityApiLookup<AccessoriesCapability, Void> CAPABILITY = EntityApiLookup.get(Accessories.of("capability"), AccessoriesCapability.class, Void.class);

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolderImpl>builder()
                .initializer(AccessoriesHolderImpl::of)
                .persistent(InstanceCodecable.constructed(AccessoriesHolderImpl::new))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("inventory_holder"));
    }

    @Override
    public void onInitialize() {
        Accessories.registerMenuType();
        Accessories.setupConfig();
        Accessories.registerCriteria();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Accessories.registerCommands(dispatcher);
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            var holder = AccessoriesEventHandler.attemptEquipFromUse(player, hand);

            if(holder.getResult().consumesAction()) {
                player.setItemInHand(hand, holder.getObject());
            }

            return holder;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> AccessoriesEventHandler.attemptEquipOnEntity(player, hand, entity));

        AccessoriesFabricNetworkHandler.INSTANCE.register();
        AccessoriesFabricNetworkHandler.INSTANCE.init();

        ServerLivingEntityEvents.AFTER_DEATH.register(AccessoriesEventHandler::onDeath);

        ServerTickEvents.START_WORLD_TICK.register(AccessoriesEventHandler::onWorldTick);

        //ServerTickEvents.END_WORLD_TICK.register(world -> ImplementedEvents.clearEndermanAngryCache());

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

                    var slots = EntitySlotLoader.getEntitySlots(livingEntity);

                    if(slots.isEmpty()) return null;

                    return new AccessoriesCapabilityImpl(livingEntity);
                }, entityType);
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if(!(entity instanceof LivingEntity livingEntity)) return;

            AccessoriesEventHandler.entityLoad(livingEntity, world);
        });

        ExtraEntityTrackingEvents.POST_START_TRACKING.register((trackedEntity, player) -> {
            if(!(trackedEntity instanceof LivingEntity livingEntity)) return;

            AccessoriesEventHandler.onTracking(livingEntity, player);
        });

        DataLoaderBase.INSTANCE = new DataLoaderImpl();

        DataLoaderBase.INSTANCE.registerListeners();
    }
}