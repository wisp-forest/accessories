package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.endec.format.nbt.NbtDeserializer;
import io.wispforest.accessories.endec.format.nbt.NbtEndec;
import io.wispforest.accessories.endec.format.nbt.NbtSerializer;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.InstanceEndec;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmElement;
import io.wispforest.endec.format.edm.EdmEndec;
import io.wispforest.endec.format.edm.LenientEdmDeserializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LootingLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Consumer;

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
                        .build()
        );
    }

    public static IEventBus BUS;

    public AccessoriesForge(final IEventBus eventBus) {
        //Accessories.init();

        AccessoriesForge.BUS = eventBus;

        Accessories.init();

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

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        eventBus.addListener(this::addDefaultDataComponents);

        NeoForge.EVENT_BUS.addListener(this::attemptEquipFromUse);
        NeoForge.EVENT_BUS.addListener(this::attemptEquipOnEntity);
    }

    public void addDefaultDataComponents(ModifyDefaultComponentsEvent event) {
        AccessoriesDataComponents.adjustDefaultComponents((itemPredicate, additionCallbackConsumer) -> {
            event.modifyMatching(itemPredicate, builder -> additionCallbackConsumer.accept(builder::set));
        });
    }

    public void attemptEquipFromUse(PlayerInteractEvent.RightClickItem event){
        var resultHolder = AccessoriesEventHandler.attemptEquipFromUse(event.getEntity(), event.getHand());

        if(resultHolder.getResult().consumesAction()) {
            event.getEntity().setItemInHand(event.getHand(), resultHolder.getObject());

            event.setCancellationResult(resultHolder.getResult());
        }
    }

    public void attemptEquipOnEntity(PlayerInteractEvent.EntityInteract event) {
        AccessoriesEventHandler.attemptEquipOnEntity(event.getEntity(), event.getHand(), event.getTarget());
    }

    public void registerCommands(RegisterCommandsEvent event) {
        Accessories.registerCommands(event.getDispatcher());
    }

    public void registerStuff(RegisterEvent event){
        event.register(Registries.MENU, (helper) -> Accessories.registerMenuType());
        event.register(Registries.TRIGGER_TYPE, (helper) -> Accessories.registerCriteria());
        event.register(Registries.DATA_COMPONENT_TYPE, (helper) -> AccessoriesDataComponents.init());
        event.register(Registries.COMMAND_ARGUMENT_TYPE, (helper) -> Accessories.registerCommandArgTypes());
    }

    public void onEntityDeath(LivingDeathEvent event){
        AccessoriesEventHandler.onDeath(event.getEntity(), event.getSource());
    }

    public void onLivingEntityTick(EntityTickEvent.Pre event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.onLivingEntityTick(livingEntity);
    }

    public void onDataSync(OnDatapackSyncEvent event){
        AccessoriesEventHandler.dataSync(event.getPlayerList(), event.getPlayer());
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

    public void onEntityLoad(EntityJoinLevelEvent event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.entityLoad(livingEntity, event.getLevel());
    }

    public void onStartTracking(PlayerEvent.StartTracking event){
        if(!(event.getTarget() instanceof LivingEntity livingEntity)) return;

        AccessoriesEventHandler.onTracking(livingEntity, (ServerPlayer) event.getEntity());
    }

    public void registerReloadListeners(AddReloadListenerEvent event){
        intermediateRegisterListeners(event::addListener);

        AccessoriesInternalsImpl.setContext(event.getConditionContext());
    }

    // This exists as a way to register things within the TCLayer without depending on NeoForge to do such within a mixin
    public void intermediateRegisterListeners(Consumer<PreparableReloadListener> registrationMethod){
//        for (ModFileScanData data : ModList.get().getAllScanData()) {
//            data.getAnnotations().forEach(annotationData -> {
//                if (annotationData.annotationType().equals(Type.getType(DataLoadingModifications.DataLoadingModificationsCapable.class))) {
//                    try {
//                        Class<?> clazz = Class.forName(annotationData.memberName());
//
//                        if (DataLoadingModifications.class.isAssignableFrom(clazz)) {
//                            try {
//                                var instance = (DataLoadingModifications) clazz.getDeclaredConstructor().newInstance();
//
//                                instance.beforeRegistration(registrationMethod);
//                            } catch (Throwable e) {
//                                LOGGER.error("Failed to load DataLoadingModificationsCapable: " + annotationData.memberName(), e);
//                            }
//                        }
//                    } catch (Throwable e) {
//                        LOGGER.error("No class from such annotation: " + annotationData.memberName(), e);
//                    }
//                }
//            });
//        }

        registrationMethod.accept(SlotTypeLoader.INSTANCE);
        registrationMethod.accept(EntitySlotLoader.INSTANCE);
        registrationMethod.accept(SlotGroupLoader.INSTANCE);

        registrationMethod.accept(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesEventHandler.dataReloadOccured = true;

                AccessoriesInternalsImpl.setContext(null);
            }
        });
    }

    //--

    public void adjustLooting(LootingLevelEvent event){
        event.setLootingLevel(ExtraEventHandler.lootingAdjustments(event.getEntity(), event.getDamageSource(), event.getLootingLevel()));
    }

    public void onWorldTick(LevelTickEvent.Pre event){
        AccessoriesEventHandler.onWorldTick(event.getLevel());
    }
}
