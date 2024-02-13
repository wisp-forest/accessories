package io.wispforest.accessories.neoforge;

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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LootingLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Accessories.MODID)
public class AccessoriesForge {

    public static final AttachmentType<AccessoriesHolder> HOLDER_ATTACHMENT_TYPE;

    public static final EntityCapability<AccessoriesCapability, Void> CAPABILITY = EntityCapability.createVoid(Accessories.of("capability"), AccessoriesCapability.class);

    static {
        HOLDER_ATTACHMENT_TYPE = Registry.register(
                NeoForgeRegistries.ATTACHMENT_TYPES,
                Accessories.of("inventory_holder"),
                AttachmentType.<AccessoriesHolder>builder(AccessoriesHolderImpl::of)
                        .serialize(InstanceCodecable.constructed(AccessoriesHolderImpl::new))
                        .copyOnDeath()
                        .build()
        );
    }

    public AccessoriesForge(final IEventBus eventBus) {
        //Accessories.init();

        NeoForge.EVENT_BUS.addListener(this::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(this::onLivingEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onDataSync);
        eventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(this::onEntityLoad);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);
        NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);

        eventBus.addListener(this::registerStuff);

        NeoForge.EVENT_BUS.addListener(this::adjustLooting);
        NeoForge.EVENT_BUS.addListener(this::onWorldTick);

        eventBus.register(AccessoriesForgeNetworkHandler.INSTANCE);
    }

    public void registerStuff(RegisterEvent event){
        event.register(Registries.MENU, (helper) -> {
            Accessories.init();
        });
    }

    public void onEntityDeath(LivingDeathEvent event){
        AccessoriesEventHandler.onDeath(event.getEntity(), event.getSource());
    }

    public void onLivingEntityTick(LivingEvent.LivingTickEvent event){
        AccessoriesEventHandler.onLivingEntityTick(event.getEntity());
    }

    public void onDataSync(OnDatapackSyncEvent event){
        AccessoriesEventHandler.dataSync(event.getPlayerList(), event.getPlayer());
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event){
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if(event.isEntityRegistered(CAPABILITY, entityType)) continue;

            event.registerEntity(CAPABILITY, entityType, (entity, unused) -> {
                if(!(entity instanceof LivingEntity livingEntity)) return null;

                var slots = AccessoriesAPI.getEntitySlots(livingEntity);

                if(slots.isEmpty()) return null;

                return new AccessoriesCapabilityImpl(livingEntity);
            });
        }
    }

    public void onEntityLoad(EntityJoinLevelEvent event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.entityLoad(livingEntity, event.getLevel());
    }

    public void onStartTracking(PlayerEvent.StartTracking event){
        if(!(event.getTarget() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.onTracking(livingEntity, (ServerPlayer) event.getEntity());
    }

    public void registerReloadListeners(AddReloadListenerEvent event){
        event.addListener(SlotTypeLoader.INSTANCE);
        event.addListener(EntitySlotLoader.INSTANCE);
        event.addListener(SlotGroupLoader.INSTANCE);

        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) { AccessoriesEventHandler.dataReloadOccured = true; }
        });
    }

    //--

    public void adjustLooting(LootingLevelEvent event){
        event.setLootingLevel(ImplementedEvents.lootingAdjustments(event.getEntity(), event.getDamageSource(), event.getLootingLevel()));
    }

    public void onWorldTick(TickEvent.LevelTickEvent event){
        if(event.phase == TickEvent.Phase.END) {
            ImplementedEvents.clearEndermanAngryCache();
        } else {
            AccessoriesEventHandler.onWorldTick(event.level);
        }
    }
}
