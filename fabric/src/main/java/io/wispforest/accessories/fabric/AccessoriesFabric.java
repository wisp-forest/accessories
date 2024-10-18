package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.DataLoaderBase;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.commands.AccessoriesCommands;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.InstanceEndec;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.InvalidateEntityCache;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public class AccessoriesFabric implements ModInitializer {

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE;

    public static final EntityApiLookup<AccessoriesCapability, Void> CAPABILITY = EntityApiLookup.get(Accessories.of("capability"), AccessoriesCapability.class, Void.class);

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolderImpl>builder()
                .initializer(AccessoriesHolderImpl::of)
                .persistent(CodecUtils.toCodec(InstanceEndec.constructed(AccessoriesHolderImpl::new)))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("inventory_holder"));
    }

    @Override
    public void onInitialize() {
        Accessories.init();

        AccessoriesNetworking.init();

        AccessoriesDataComponents.init();

        AccessoriesMenuTypes.registerMenuType();
        Accessories.registerCriteria();
        AccessoriesCommands.registerCommandArgTypes();

        ArmorSlotTypes.INSTANCE.registerAccessories((consumer) -> {
            RegistryEntryAddedCallback.event(BuiltInRegistries.ITEM).register(consumer::accept);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AccessoriesCommands.registerCommands(dispatcher, registryAccess);
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            var holder = AccessoriesEventHandler.attemptEquipFromUse(player, hand);

            if(holder.getResult().consumesAction()) player.setItemInHand(hand, holder.getObject());

            return holder;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> AccessoriesEventHandler.attemptEquipOnEntity(player, hand, entity));

        ServerTickEvents.START_WORLD_TICK.register(AccessoriesEventHandler::onWorldTick);

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

        DefaultItemComponentEvents.MODIFY.register(context -> {
            AccessoriesEventHandler.setupItems(new AccessoriesEventHandler.AddDataComponentCallback() {
                @Override
                public <T> void addTo(Item item, DataComponentType<T> componentType, T component) {
                    context.modify(item, builder -> builder.set(componentType, component));
                }
            });
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            AccessoriesNetworking.CHANNEL.serverHandle(player).send(new InvalidateEntityCache(player.getId()));
        });
    }
}