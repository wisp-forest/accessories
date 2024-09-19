package io.wispforest.cclayer;

import com.google.common.collect.HashMultimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.AdjustAttributeModifierCallback;
import io.wispforest.accessories.api.events.CanEquipCallback;
import io.wispforest.accessories.api.events.CanUnequipCallback;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.*;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.common.CuriosHelper;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.capability.CurioItemHandler;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;
import top.theillusivec4.curios.compat.WrappedAccessory;
import top.theillusivec4.curios.compat.WrappedSlotType;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;
import top.theillusivec4.curios.server.SlotHelper;
import top.theillusivec4.curios.server.command.CurioArgumentType;

import java.util.HashSet;
import java.util.Set;

@Mod(value = CCLayer.MODID)
public class CCLayer {

    public static final String MODID = "cclayer";

    public CCLayer(IEventBus eventBus){
        eventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(this::serverAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);

        CuriosApi.setCuriosHelper(new CuriosHelper());

        //ModList.get().isLoaded("curios");

        CuriosRegistry.init(eventBus);

        AccessoryChangeCallback.EVENT.register((prevStack, currentStack, reference, stateChange) -> {
            NeoForge.EVENT_BUS.post(new CurioChangeEvent(reference.entity(), reference.slotName(), reference.slot(), prevStack, currentStack));
        });

        DeathWrapperEventsImpl.init();

        CanEquipCallback.EVENT.register((stack, reference) -> {
            var event = new CurioCanEquipEvent(stack, CuriosWrappingUtils.create(reference));

            NeoForge.EVENT_BUS.post(event);

            return CuriosWrappingUtils.convert(event.getEquipResult());
        });

        CanUnequipCallback.EVENT.register((stack, reference) -> {
            var event = new CurioCanUnequipEvent(stack, CuriosWrappingUtils.create(reference));

            NeoForge.EVENT_BUS.post(event);

            return CuriosWrappingUtils.convert(event.getUnequipResult());
        });

        AdjustAttributeModifierCallback.EVENT.register((stack, reference, builder) -> {
            var modifiers = HashMultimap.<Holder<Attribute>, AttributeModifier>create();

            var event = new CurioAttributeModifierEvent(stack, CuriosWrappingUtils.create(reference), ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, AccessoryAttributeBuilder.createSlotPath(reference)), modifiers);

            NeoForge.EVENT_BUS.post(event);

            modifiers.clear();
            modifiers.putAll(event.getModifiers());
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            event.registerEntity(CuriosCapability.ITEM_HANDLER, entityType,
                    (entity, ctx) -> {
                        if (entity instanceof LivingEntity livingEntity && !EntitySlotLoader.getEntitySlots(livingEntity).isEmpty()) {
                            return new CurioItemHandler(livingEntity);
                        }

                        return null;
                    });

            event.registerEntity(CuriosCapability.INVENTORY, entityType,
                    (entity, ctx) -> {
                        if (entity instanceof LivingEntity livingEntity) {
                            var capability = AccessoriesCapability.get(livingEntity);

                            if(capability != null) return new WrappedCurioItemHandler((AccessoriesCapabilityImpl) capability);
                        }

                        return null;
                    });
        }

        for (Item item : BuiltInRegistries.ITEM) {
            // Force all items instanceof ICurioItem to register for Accessories systems
            if(!CuriosImplMixinHooks.REGISTRY.containsKey(item) && item instanceof ICurioItem iCurioItem){
                CuriosImplMixinHooks.registerCurio(item, iCurioItem);
            }

            event.registerItem(CuriosCapability.ITEM, BASE_PROVIDER, item);
        }
    }

    public static final ICapabilityProvider<ItemStack, Void, ICurio> BASE_PROVIDER = (stack, ctx) -> {
        Item it = stack.getItem();
        ICurioItem curioItem = CuriosImplMixinHooks.getCurioFromRegistry(stack).orElse(null);

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
    };

    private void serverAboutToStart(ServerAboutToStartEvent evt) {
        CuriosApi.setSlotHelper(new SlotHelper());
        Set<String> slotIds = new HashSet<>();

        SlotTypeLoader.INSTANCE.getSlotTypes(false).values()
                .stream()
                .map(WrappedSlotType::new)
                .forEach(value -> {
                    CuriosApi.getSlotHelper().addSlotType(value);
                    slotIds.add(value.getIdentifier());
                });
        CurioArgumentType.slotIds = slotIds;
    }

    private void serverStopped(ServerStoppedEvent evt) {
        CuriosApi.setSlotHelper(null);
    }
}
