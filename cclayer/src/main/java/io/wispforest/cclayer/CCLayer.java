package io.wispforest.cclayer;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.DataLoadingModifications;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.common.CuriosHelper;
import top.theillusivec4.curios.common.capability.CurioItemHandler;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.common.slottype.LegacySlotManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedAccessory;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;
import top.theillusivec4.curios.server.SlotHelper;
import top.theillusivec4.curios.server.command.CurioArgumentType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Mod(value = CCLayer.MODID)
public class CCLayer {

    public static final String MODID = "cclayer";

    public CCLayer(IEventBus eventBus){
        eventBus.addListener(this::registerCapabilities);
        eventBus.addListener(this::process);
        NeoForge.EVENT_BUS.addListener(this::serverAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);

        CuriosApi.setCuriosHelper(new CuriosHelper());

        //ModList.get().isLoaded("curios");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            event.registerEntity(CuriosCapability.ITEM_HANDLER, entityType,
                    (entity, ctx) -> {
                        if (entity instanceof LivingEntity livingEntity && EntitySlotLoader.getEntitySlots(livingEntity).isEmpty()) {
                            return new CurioItemHandler(livingEntity);
                        }

                        return null;
                    });

            event.registerEntity(CuriosCapability.INVENTORY, entityType,
                    (entity, ctx) -> {
                        if (entity instanceof LivingEntity livingEntity) {
                            var capability = AccessoriesCapability.get(livingEntity);

                            if(capability.isPresent()) return new WrappedCurioItemHandler((AccessoriesCapabilityImpl) capability.get());
                        }

                        return null;
                    });
        }

        for (Item item : BuiltInRegistries.ITEM) {
            // Force all items instanceof ICurioItem to register for Accessories systems
            if(CuriosImplMixinHooks.getCurioFromRegistry(item).isEmpty() && item instanceof ICurioItem iCurioItem){
                CuriosImplMixinHooks.registerCurio(item, iCurioItem);
            }

            event.registerItem(CuriosCapability.ITEM, (stack, ctx) -> {
                Item it = stack.getItem();
                ICurioItem curioItem = CuriosImplMixinHooks.getCurioFromRegistry(item).orElse(null);

                if (curioItem == null && it instanceof ICurioItem itemCurio) {
                    curioItem = itemCurio;
                }

                if(curioItem == null){
                    var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

                    curioItem = new WrappedAccessory(accessory);
                }

                if (curioItem != null && curioItem.hasCurioCapability(stack)) {
                    return new ItemizedCurioCapability(curioItem, stack);
                }

                return null;
            }, item);
        }
    }

    private void serverAboutToStart(ServerAboutToStartEvent evt) {
        CuriosApi.setSlotHelper(new SlotHelper());
        Set<String> slotIds = new HashSet<>();

        for (ISlotType value : CuriosSlotManager.INSTANCE.getSlots().values()) {
            CuriosApi.getSlotHelper().addSlotType(value);
            slotIds.add(value.getIdentifier());
        }
        CurioArgumentType.slotIds = slotIds;
    }

    private void serverStopped(ServerStoppedEvent evt) {
        CuriosApi.setSlotHelper(null);
    }

    private void process(InterModProcessEvent evt) {
        LegacySlotManager.buildImcSlotTypes(evt.getIMCStream(SlotTypeMessage.REGISTER_TYPE::equals),
                evt.getIMCStream(SlotTypeMessage.MODIFY_TYPE::equals));
    }

    @DataLoadingModifications.DataLoadingModificationsCapable
    public static class DataLoadingModificationsImpl implements DataLoadingModifications {

        public DataLoadingModificationsImpl(){}

        private static boolean addedEventHooks = false;

        @Override
        public void beforeRegistration(Consumer<PreparableReloadListener> registrationMethod) {
            registrationMethod.accept(CuriosSlotManager.INSTANCE);
            registrationMethod.accept(CuriosEntityManager.INSTANCE);

            if (!addedEventHooks) {
                SlotTypeLoader.INSTANCE.externalEventHooks.add(map -> {
                    for (var entry : CuriosSlotManager.INSTANCE.slotTypeBuilders.entrySet()) {
                        var accessoryType = CuriosWrappingUtils.curiosToAccessories(entry.getKey());

                        if (!map.containsKey(accessoryType)) {
                            var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                            var curiosBuilder = entry.getValue();

                            if (curiosBuilder.size != null) {
                                builder.amount(curiosBuilder.size);
                            }

                            if (curiosBuilder.sizeMod != 0) {
                                builder.addAmount(curiosBuilder.sizeMod);
                            }

                            if (curiosBuilder.icon != null) {
                                builder.icon(curiosBuilder.icon);
                            }

                            if (curiosBuilder.order != null) {
                                builder.order(curiosBuilder.order);
                            }

                            if (curiosBuilder.dropRule != null) {
                                builder.dropRule(CuriosWrappingUtils.convert(curiosBuilder.dropRule));
                            }

                            builder.alternativeTranslation("curios.identifier." + entry.getKey());

                            map.put(accessoryType, builder.create());
                        }
                    }
                });

                EntitySlotLoader.INSTANCE.externalEventHooks.add(map -> {
                    for (var entry : CuriosEntityManager.INSTANCE.entityTypeSlotData.entrySet()) {
                        var slotTypes = map.computeIfAbsent(entry.getKey(), entityType -> new HashMap<>());

                        for (String s : entry.getValue().build()) {
                            var typeid = CuriosWrappingUtils.curiosToAccessories(s);

                            if (!slotTypes.containsKey(typeid)) {
                                var type = SlotTypeLoader.INSTANCE.getSlotTypes(false).get(typeid);

                                //TODO: ERROR ABOUT INFO?
                                if (type != null) {
                                    slotTypes.put(typeid, type);
                                }
                            }
                        }
                    }
                });

                addedEventHooks = true;
            }
        }
    }
}
