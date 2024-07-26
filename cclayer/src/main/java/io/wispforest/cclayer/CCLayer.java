package io.wispforest.cclayer;

import com.google.common.collect.HashMultimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.AdjustAttributeModifierCallback;
import io.wispforest.accessories.api.events.CanEquipCallback;
import io.wispforest.accessories.api.events.CanUnequipCallback;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.utils.AttributeUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.*;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.CuriosHelper;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.capability.CurioInventoryCapability;
import top.theillusivec4.curios.common.capability.CurioItemHandler;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;
import top.theillusivec4.curios.compat.WrappedAccessory;
import top.theillusivec4.curios.compat.WrappedICurioProvider;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;
import top.theillusivec4.curios.server.SlotHelper;
import top.theillusivec4.curios.server.command.CurioArgumentType;

import java.util.HashSet;
import java.util.Set;

@Mod(value = CCLayer.MODID)
public class CCLayer {

    public static final String MODID = "cclayer";

    public CCLayer(){
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntitiesCapabilities);
        MinecraftForge.EVENT_BUS.addListener(this::registerCapabilities);

        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldTick);


        CuriosApi.setCuriosHelper(new CuriosHelper());

        CuriosRegistry.init();

        AccessoryChangeCallback.EVENT.register((prevStack, currentStack, reference, stateChange) -> {
            MinecraftForge.EVENT_BUS.post(new CurioChangeEvent(reference.entity(), reference.slotName(), reference.slot(), prevStack, currentStack));
        });

        DeathWrapperEventsImpl.init();

        CanEquipCallback.EVENT.register((stack, reference) -> {
            var event = new CurioEquipEvent(stack, CuriosWrappingUtils.create(reference));

            MinecraftForge.EVENT_BUS.post(event);

            return CuriosWrappingUtils.convert(event.getEquipResult());
        });

        CanUnequipCallback.EVENT.register((stack, reference) -> {
            var event = new CurioUnequipEvent(stack, CuriosWrappingUtils.create(reference));

            MinecraftForge.EVENT_BUS.post(event);

            return CuriosWrappingUtils.convert(event.getUnequipResult());
        });

        AdjustAttributeModifierCallback.EVENT.register((stack, reference, builder) -> {
            var modifiers = HashMultimap.<Attribute, AttributeModifier>create();

            var data = AttributeUtils.getModifierData(new ResourceLocation(CuriosConstants.MOD_ID, AccessoryAttributeBuilder.createSlotPath(reference)));

            var event = new CurioAttributeModifierEvent(stack, CuriosWrappingUtils.create(reference), data.right(), modifiers);

            MinecraftForge.EVENT_BUS.post(event);

            modifiers.clear();
            modifiers.putAll(event.getModifiers());
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICuriosItemHandler.class);
        event.register(ICurio.class);
    }

    public void attachEntitiesCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity livingEntity) {
            event.addCapability(CuriosCapability.ID_INVENTORY, CurioInventoryCapability.createProvider(livingEntity));
        }
    }

    private boolean attemptRegister = false;

    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if(!event.phase.equals(TickEvent.Phase.START) || attemptRegister) return;

        for (Item item : ForgeRegistries.ITEMS) {
            var defaultStack = item.getDefaultInstance();

            if(CuriosImplMixinHooks.getCurioFromRegistry(item).isEmpty() && item instanceof ICurioItem iCurioItem){
                CuriosImplMixinHooks.registerCurio(item, iCurioItem);

                continue;
            }

            if(AccessoriesAPI.getAccessory(item) != null) {
                continue;
            }

            if(defaultStack.getCapability(CuriosCapability.ITEM).isPresent()) {
                AccessoriesAPI.registerAccessory(item, new WrappedICurioProvider());
            }
        }

        attemptRegister = true;
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
}
