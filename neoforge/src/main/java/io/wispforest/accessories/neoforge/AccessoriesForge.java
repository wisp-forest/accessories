package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.commands.AccessoriesCommands;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.endec.format.nbt.NbtDeserializer;
import io.wispforest.accessories.endec.format.nbt.NbtSerializer;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.InstanceEndec;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mod(Accessories.MODID)
public class AccessoriesForge {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE;

    public static final EntityCapability<AccessoriesCapability, Void> CAPABILITY = EntityCapability.createVoid(Accessories.of("capability"), AccessoriesCapability.class);

    static {
        HOLDER_ATTACHMENT_TYPE = Registry.register(
                NeoForgeRegistries.ATTACHMENT_TYPES,
                Accessories.of("inventory_holder"),
                AttachmentType.builder(AccessoriesHolderImpl::of)
                        .serialize(new IAttachmentSerializer<>() {
                            private final Endec<AccessoriesHolderImpl> ENDEC = InstanceEndec.constructed(AccessoriesHolderImpl::new);

                            @Override
                            public AccessoriesHolderImpl read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                                return ENDEC.decodeFully(
                                        SerializationContext.attributes(RegistriesAttribute.of((RegistryAccess) provider)),
                                        NbtDeserializer::of,
                                        tag);
                            }

                            @Override
                            @Nullable
                            public Tag write(AccessoriesHolderImpl object, HolderLookup.Provider provider) {
                                return ENDEC.encodeFully(
                                        SerializationContext.attributes(RegistriesAttribute.of((RegistryAccess) provider)),
                                        NbtSerializer::of,
                                        object);
                            }
                        })
                        .copyOnDeath()
                        .build());
    }

    public static IEventBus BUS;

    public AccessoriesForge(final IEventBus eventBus) {
        AccessoriesForge.BUS = eventBus;

        Accessories.init();

        NeoForge.EVENT_BUS.addListener(this::attemptEquipFromUse);
        NeoForge.EVENT_BUS.addListener(this::attemptEquipOnEntity);
        NeoForge.EVENT_BUS.addListener(this::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(this::onLivingEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onDataSync);
        NeoForge.EVENT_BUS.addListener(this::onEntityLoad);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);
        NeoForge.EVENT_BUS.addListener(this::onWorldTick);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        eventBus.addListener(AccessoriesForgeNetworkHandler.INSTANCE::initializeNetworking);
        eventBus.addListener(this::registerStuff);

        NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);

        eventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            // A hack to deal with player data not being transferred when a ClientboundRespawnPacket occurs for teleporting between two dimensions
            if(!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

            AccessoriesEventHandler.onTracking(serverPlayer, serverPlayer);
        });

        eventBus.addListener((ModifyDefaultComponentsEvent event) -> {
            AccessoriesEventHandler.setupItems(new AccessoriesEventHandler.AddDataComponentCallback() {
                @Override
                public <T> void addTo(Item item, DataComponentType<T> componentType, T component) {
                    event.modify(item, builder -> builder.set(componentType, component));
                }
            });
        });
    }

    //--

    public void registerCommands(RegisterCommandsEvent event) {
        AccessoriesCommands.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    public void registerStuff(RegisterEvent event){
        event.register(Registries.MENU, (helper) -> AccessoriesMenuTypes.registerMenuType());
        event.register(Registries.TRIGGER_TYPE, (helper) -> Accessories.registerCriteria());
        event.register(Registries.DATA_COMPONENT_TYPE, (helper) -> AccessoriesDataComponents.init());
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

    public void registerCapabilities(RegisterCapabilitiesEvent event){
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if(event.isEntityRegistered(CAPABILITY, entityType)) continue;

            event.registerEntity(CAPABILITY, entityType, (entity, unused) -> {
                if(!(entity instanceof LivingEntity livingEntity)) return null;

                var slots = EntitySlotLoader.getEntitySlots(livingEntity);

                if(slots.isEmpty()) return null;

                return new AccessoriesCapabilityImpl(livingEntity);
            });
        }
    }

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
        var droppedStacks = AccessoriesEventHandler.onDeath(event.getEntity(), event.getSource());

        if (droppedStacks == null) return;

        event.getDrops().addAll(
                droppedStacks.stream().flatMap(itemStack -> {
                    var pos = event.getEntity().position();

                    return getItemEntities(event.getEntity().level(), pos.x, pos.y, pos.z, itemStack);
                }).toList()
        );
    }

    private static Stream<ItemEntity> getItemEntities(Level level, double x, double y, double z, ItemStack stack) {
        double d = EntityType.ITEM.getWidth();

        double e = 1.0 - d;
        double f = d / 2.0;

        double g = Math.floor(x) + level.random.nextDouble() * e + f;
        double h = Math.floor(y) + level.random.nextDouble() * e;
        double i = Math.floor(z) + level.random.nextDouble() * e + f;

        var itemEntities = new ArrayList<ItemEntity>();

        while(!stack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(level, g, h, i, stack.split(level.random.nextInt(21) + 10));
            itemEntity.setDeltaMovement(level.random.triangle(0.0, 0.11485000171139836), level.random.triangle(0.2, 0.11485000171139836), level.random.triangle(0.0, 0.11485000171139836));
            itemEntities.add(itemEntity);
        }

        return itemEntities.stream();
    }

    public void onLivingEntityTick(EntityTickEvent.Pre event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.onLivingEntityTick(livingEntity);
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

    public void onWorldTick(LevelTickEvent.Pre event){
        AccessoriesEventHandler.onWorldTick(event.getLevel());
    }
}
