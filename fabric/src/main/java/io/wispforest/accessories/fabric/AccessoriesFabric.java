package io.wispforest.accessories.fabric;

import com.google.common.reflect.Reflection;
import com.mojang.brigadier.arguments.ArgumentType;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.commands.api.ArgumentRegistrationCallback;
import io.wispforest.accessories.commands.api.CommandGenerators;
import io.wispforest.accessories.commands.api.core.RecordArgumentTypeInfo;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.api.SyncedDataHelperManager;
import io.wispforest.accessories.impl.core.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.event.AccessoriesEventHandler;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.accessories.misc.AccessoriesGameRules;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.InvalidateEntityCache;
import io.wispforest.accessories.networking.client.SyncEntireContainer;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.accessories.utils.ServerInstanceHolder;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;

import java.util.Objects;

public class AccessoriesFabric implements ModInitializer {

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE;
    public static final AttachmentType<AccessoriesPlayerOptionsHolder> PLAYER_OPTIONS_ATTACHMENT_TYPE;

    public static final EntityApiLookup<AccessoriesCapability, Void> CAPABILITY = EntityApiLookup.get(Accessories.of("capability"), AccessoriesCapability.class, Void.class);

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolderImpl>builder()
                .initializer(AccessoriesHolderImpl::of)
                .persistent(CodecUtils.toCodec(EndecUtils.createMapCarrierEndec(AccessoriesHolderImpl::new)))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("inventory_holder"));

        PLAYER_OPTIONS_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesPlayerOptionsHolder>builder()
                .initializer(AccessoriesPlayerOptionsHolder::new)
                .persistent(CodecUtils.toCodec(EndecUtils.createMapCarrierEndec(AccessoriesPlayerOptionsHolder::new)))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("player_options"));
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerInstanceHolder::setInstance);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerInstanceHolder.setInstance(() -> null));

        Accessories.init();

        AccessoriesNetworking.init();

        SyncedDataHelperManager.init(AccessoriesNetworking.CHANNEL, playerConsumer -> {
            ResourceLocation beforeDefaultPhase = Accessories.of("before_default_phase");

            ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(beforeDefaultPhase, Event.DEFAULT_PHASE);

            ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(beforeDefaultPhase, (player, joined) -> playerConsumer.accept(player));
        });

        AccessoriesDataComponents.init();

        AccessoriesMenuTypes.registerMenuType();
        Accessories.registerCriteria();
        CommandGenerators.registerAllArgumentTypes(new ArgumentRegistrationCallback() {
            @Override
            public <A extends ArgumentType<?>, T> RecordArgumentTypeInfo<A, T> register(ResourceLocation location, Class<A> clazz, RecordArgumentTypeInfo<A, T> info) {
                ArgumentTypeRegistry.registerArgumentType(location, clazz, info);

                return info;
            }
        });

        CommandRegistrationCallback.EVENT.register(CommandGenerators::registerAllGenerators);

        UseItemCallback.EVENT.register((player, level, hand) -> {
            //if (level.isClientSide) return InteractionResult.PASS;

            var holder = AccessoriesEventHandler.attemptEquipFromUse(player, hand);

            if (holder instanceof InteractionResult.Success && level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            //TODO: CONFIRM IF THIS IS CORRECT!
            if(holder instanceof InteractionResult.Success success) {
                var stack = Objects.requireNonNullElse(success.heldItemTransformedTo(), player.getItemInHand(hand));

                player.setItemInHand(hand, stack);
            }

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

        ResourceLoader.get(PackType.SERVER_DATA)
            .registerReloader(
                Accessories.DATA_RELOAD_HOOK,
                (ResourceManagerReloadListener) manager -> AccessoriesEventHandler.dataReloadOccurred = true
            );

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

            AccessoriesEventHandler.onTracking(player, player);
        });

        var afterDefault = Accessories.of("after_default");

        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, afterDefault);

        ServerPlayerEvents.AFTER_RESPAWN.register(afterDefault, (oldPlayer, newPlayer, alive) -> {
            // Required due to mods possibly causing a desync between server and client as transfer of data attachments may have not occured yet
            // as fabric dose such after respawn which is after entity load event
            SyncEntireContainer.syncToAllTrackingAndSelf(newPlayer);
        });

        Reflection.initialize(AccessoriesGameRules.class);
    }
}