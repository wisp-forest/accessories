package io.wispforest.accessories.neoforge;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.commands.api.ArgumentRegistrationCallback;
import io.wispforest.accessories.commands.api.CommandGenerators;
import io.wispforest.accessories.commands.api.core.RecordArgumentTypeInfo;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.core.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.event.AccessoriesEventHandler;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.accessories.data.api.SyncedDataHelperManager;
import io.wispforest.endec.SerializationContext;
import io.wispforest.accessories.utils.InstanceEndec;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mod(Accessories.MODID)
public class AccessoriesForge {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final AttachmentType<AccessoriesHolderImpl> HOLDER_ATTACHMENT_TYPE = AttachmentType.builder(AccessoriesHolderImpl::of)
            .serialize(createSerializerFor(AccessoriesHolderImpl::new))
            .copyOnDeath()
            .build();

    public static final AttachmentType<AccessoriesPlayerOptionsHolder> PLAYER_OPTIONS_ATTACHMENT_TYPE = AttachmentType.builder(AccessoriesPlayerOptionsHolder::new)
            .serialize(createSerializerFor(AccessoriesPlayerOptionsHolder::new))
            .copyOnDeath()
            .build();

    private static <T extends InstanceEndec> IAttachmentSerializer<CompoundTag, T> createSerializerFor(Supplier<T> supplier) {
        return new IAttachmentSerializer<>() {
            @Override
            public T read(IAttachmentHolder iAttachmentHolder, CompoundTag arg, HolderLookup.Provider arg2) {
                var holder = supplier.get();

                holder.decode(new NbtMapCarrier(arg), SerializationContext.empty());

                return holder;
            }

            @Override
            public @Nullable CompoundTag write(T object, HolderLookup.Provider arg) {
                var tag = new CompoundTag();

                object.encode(new NbtMapCarrier(tag), SerializationContext.empty());

                return null;
            }
        };
    }

    public static final EntityCapability<AccessoriesCapability, Void> CAPABILITY = EntityCapability.createVoid(Accessories.of("capability"), AccessoriesCapability.class);

    public static IEventBus BUS;

    public AccessoriesForge(final IEventBus eventBus) {
        AccessoriesForge.BUS = eventBus;

        Accessories.init();

        eventBus.addListener(this::registerStuff);

        eventBus.addListener(this::registerCapabilities);

        eventBus.addListener(this::commonInit);

        NeoForge.EVENT_BUS.addListener(this::attemptEquipFromUse);
        NeoForge.EVENT_BUS.addListener(this::attemptEquipOnEntity);
        NeoForge.EVENT_BUS.addListener(this::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(this::onLivingEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onDataSync);
        NeoForge.EVENT_BUS.addListener(this::onEntityLoad);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);
        NeoForge.EVENT_BUS.addListener(this::onWorldTick);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            // A hack to deal with player data not being transferred when a ClientboundRespawnPacket occurs for teleporting between two dimensions
            if(!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

            AccessoriesEventHandler.onTracking(serverPlayer, serverPlayer);
        });

//        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
//            AccessoriesNetworking.CHANNEL.serverHandle(event.getEntity()).send(new InvalidateEntityCache(event.getEntity().getId()));
//        });

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

    public void commonInit(FMLCommonSetupEvent event) {
        AccessoriesNetworking.init();

        SyncedDataHelperManager.init(AccessoriesNetworking.CHANNEL, playerConsumer -> {
            NeoForge.EVENT_BUS.<OnDatapackSyncEvent>addListener(EventPriority.HIGHEST, syncEvent -> syncEvent.getRelevantPlayers().forEach(playerConsumer::accept));
        });

        Accessories.RULE_KEEP_ACCESSORY_INVENTORY = GameRules.register("accessories.keepAccessoryInventory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    }

    public void registerCommands(RegisterCommandsEvent event) {
        CommandGenerators.registerAllGenerators(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    public void registerStuff(RegisterEvent event){
        event.register(Registries.MENU, (helper) -> AccessoriesMenuTypes.registerMenuType());
        event.register(Registries.TRIGGER_TYPE, (helper) -> Accessories.registerCriteria());
        event.register(Registries.DATA_COMPONENT_TYPE, (helper) -> AccessoriesDataComponents.init());
        event.register(Registries.COMMAND_ARGUMENT_TYPE, (helper) -> CommandGenerators.registerAllArgumentTypes(new ArgumentRegistrationCallback() {
            @Override
            public <A extends ArgumentType<?>, T> RecordArgumentTypeInfo<A, T> register(ResourceLocation location, Class<A> clazz, RecordArgumentTypeInfo<A, T> info) {
                helper.register(location, ArgumentTypeInfos.registerByClass(clazz, info));

                return info;
            }
        }));
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, (helper) -> {
            Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, Accessories.of("inventory_holder"), HOLDER_ATTACHMENT_TYPE);
            Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, Accessories.of("player_options"), PLAYER_OPTIONS_ATTACHMENT_TYPE);
        });
    }

    public void registerReloadListeners(AddServerReloadListenersEvent event){
        var loaders = AccessoriesInternalsImpl.TO_BE_LOADED.getOrDefault(PackType.SERVER_DATA, new HashMap<>());

        loaders.forEach((endecDataLoader, setupRegistryCallback) -> {
            setupRegistryCallback.accept(event.getRegistryAccess());
            event.addListener(endecDataLoader.getId(), endecDataLoader);
        });

        loaders.forEach((endecDataLoader, providerConsumer) -> {
            for (var dependencyId : endecDataLoader.getDependencyIds()) {
                event.addDependency(dependencyId, endecDataLoader.getId());
            }
        });

        event.addListener(Accessories.DATA_RELOAD_HOOK, new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesEventHandler.dataReloadOccurred = true;
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

        if(!(resultHolder instanceof InteractionResult.Success)) return;

        event.setCancellationResult(resultHolder);
        event.setCanceled(true);
    }

    public void attemptEquipOnEntity(PlayerInteractEvent.EntityInteract event) {
        var resultHolder = AccessoriesEventHandler.attemptEquipOnEntity(event.getEntity(), event.getHand(), event.getTarget());

        if(!(resultHolder instanceof InteractionResult.Success)) return;

        event.setCancellationResult(resultHolder);
        event.setCanceled(true);
    }

    public void onEntityDeath(LivingDropsEvent event){
        var droppedStacks = AccessoriesEventHandler.onDeath(event.getEntity(), event.getSource());

        if (droppedStacks == null) return;

        event.getDrops().addAll(
                droppedStacks.stream()
                        .flatMap(stack -> createDroppedEntity(event.getEntity(), stack))
                        .toList()
        );
    }

    private static Stream<ItemEntity> createDroppedEntity(Entity entity, ItemStack stack) {
        var itemEntities = new ArrayList<ItemEntity>();

        if (!stack.isEmpty()) {
            var random = entity.getRandom();

            if (entity instanceof Player player) {
                double d = player.getEyeY() - 0.3F;

                var itemEntity = new ItemEntity(player.level(), player.getX(), d, player.getZ(), stack);

                itemEntity.setPickUpDelay(40);

                float f = random.nextFloat() * 0.5F;
                float g = random.nextFloat() * (float) (Math.PI * 2);

                itemEntity.setDeltaMovement((-Mth.sin(g) * f), 0.2F, (Mth.cos(g) * f));

                itemEntities.add(itemEntity);
            } else {
                double itemWidth = EntityType.ITEM.getWidth();

                double e = 1.0 - itemWidth;
                double f = itemWidth / 2.0;

                double itemX = Math.floor(entity.getX()) + random.nextDouble() * e + f;
                double itemY = Math.floor(entity.getY()) + random.nextDouble() * e;
                double itemZ = Math.floor(entity.getZ()) + random.nextDouble() * e + f;

                while(!stack.isEmpty()) {
                    var itemEntity = new ItemEntity(entity.level(), itemX, itemY, itemZ, stack.split(random.nextInt(21) + 10));

                    var max = 0.11485000171139836;

                    itemEntity.setDeltaMovement(random.triangle(0.0, max), random.triangle(0.2, max), random.triangle(0.0, max));

                    itemEntities.add(itemEntity);
                }
            }
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
