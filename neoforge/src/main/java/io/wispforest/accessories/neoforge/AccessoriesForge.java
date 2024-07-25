package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.commands.AccessoriesCommands;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.forge.AccessoriesInternalsImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.InstanceEndec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.util.function.Consumer;

@Mod(Accessories.MODID)
public class AccessoriesForge {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE;

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolderImpl>builder()
                .initializer(AccessoriesHolderImpl::of)
                .persistent(CodecUtils.ofEndec(InstanceEndec.constructed(AccessoriesHolderImpl::new)))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("inventory_holder"));
    }

    public static IEventBus BUS;

    public AccessoriesForge() {
        var eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        AccessoriesForge.BUS = eventBus;

        Accessories.init();

        MinecraftForge.EVENT_BUS.addListener(this::attemptEquipFromUse);
        MinecraftForge.EVENT_BUS.addListener(this::attemptEquipOnEntity);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityDeath);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingEntityTick);
        MinecraftForge.EVENT_BUS.addListener(this::onDataSync);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldTick);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        eventBus.register(AccessoriesForgeNetworkHandler.INSTANCE);
        eventBus.addListener(this::registerStuff);

        MinecraftForge.EVENT_BUS.addListener(this::registerReloadListeners);

//        eventBus.addListener(this::registerCapabilities);

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            // A hack to deal with player data not being transferred when a ClientboundRespawnPacket occurs for teleporting between two dimensions
            if(!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

            AccessoriesEventHandler.onTracking(serverPlayer, serverPlayer);
        });

        AccessoriesForgeNetworkHandler.INSTANCE.initializeNetworking();

        eventBus.addListener((FMLCommonSetupEvent event) -> Accessories.registerCriteria());
    }

    //--

    public void registerCommands(RegisterCommandsEvent event) {
        AccessoriesCommands.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    public void registerStuff(RegisterEvent event){
        event.register(Registries.MENU, (helper) -> Accessories.registerMenuType());
        event.register(Registries.COMMAND_ARGUMENT_TYPE, (helper) -> AccessoriesCommands.registerCommandArgTypes());
    }

    public void registerReloadListeners(AddReloadListenerEvent event){
        intermediateRegisterListeners(event::addListener);

        AccessoriesInternalsImpl.setContext(event.getConditionContext());
    }

    // This exists as a way to register things within the TCLayer without depending on NeoForge to do this within a mixin
    public void intermediateRegisterListeners(Consumer<PreparableReloadListener> registrationMethod){
        registrationMethod.accept(SlotTypeLoader.INSTANCE);
        registrationMethod.accept(EntitySlotLoader.INSTANCE);
        registrationMethod.accept(SlotGroupLoader.INSTANCE);

        registrationMethod.accept(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesEventHandler.dataReloadOccurred = true;

                AccessoriesInternalsImpl.setContext(null);
            }
        });
    }

//    public void registerCapabilities(RegisterCapabilitiesEvent event){
//        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
//            if(event.isEntityRegistered(CAPABILITY, entityType)) continue;
//
//            event.registerEntity(CAPABILITY, entityType, (entity, unused) -> {
//                if(!(entity instanceof LivingEntity livingEntity)) return null;
//
//                var slots = EntitySlotLoader.getEntitySlots(livingEntity);
//
//                if(slots.isEmpty()) return null;
//
//                return new AccessoriesCapabilityImpl(livingEntity);
//            });
//        }
//    }

    //--

    public void attemptEquipFromUse(PlayerInteractEvent.RightClickItem event){
        var resultHolder = AccessoriesEventHandler.attemptEquipFromUse(event.getEntity(), event.getHand());

        if(!resultHolder.getResult().consumesAction()) return;

        event.getEntity().setItemInHand(event.getHand(), resultHolder.getObject());

        event.setCancellationResult(resultHolder.getResult());
    }

    public void attemptEquipOnEntity(PlayerInteractEvent.EntityInteract event) {
        AccessoriesEventHandler.attemptEquipOnEntity(event.getEntity(), event.getHand(), event.getTarget());
    }

    public void onEntityDeath(LivingDropsEvent event){
        AccessoriesEventHandler.onDeath(event.getEntity(), event.getSource());
    }

    public void onLivingEntityTick(LivingEvent.LivingTickEvent event){
        //if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        AccessoriesEventHandler.onLivingEntityTick(event.getEntity());
    }

    public void onDataSync(OnDatapackSyncEvent event){
        var player = event.getPlayer();

        AccessoriesEventHandler.dataSync(player == null ? event.getPlayerList() : null, player);
    }

    public void onEntityLoad(EntityJoinLevelEvent event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.entityLoad(livingEntity, event.getLevel());
    }

    public void onStartTracking(PlayerEvent.StartTracking event){
        if(!(event.getTarget() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.onTracking(livingEntity, (ServerPlayer) event.getEntity());
    }

    public void onWorldTick(TickEvent.LevelTickEvent event){
        if(!event.phase.equals(TickEvent.Phase.START)) return;

        AccessoriesEventHandler.onWorldTick(event.level);
    }
}
