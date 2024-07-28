package io.wispforest.cclayer.client;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.cclayer.CCLayer;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ClientNeoForgeMod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.type.util.IIconHelper;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

@EventBusSubscriber(modid = CCLayer.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CCLayerClient {

    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event){
        var bus = ModLoadingContext.get().getActiveContainer().getEventBus();

        bus.addListener(CCLayerClient::loaderRenders);
        CuriosApi.setIconHelper(new IIconHelper() {
            @Override public void clearIcons() {}
            @Override public void addIcon(String identifier, ResourceLocation resourceLocation) {}

            @Override
            public ResourceLocation getIcon(String identifier) {
                var slot = SlotTypeLoader.INSTANCE.getSlotTypes(true).get(CuriosWrappingUtils.curiosToAccessories(identifier));

                return slot != null ? slot.icon() : SlotType.EMPTY_SLOT_ICON;
            }
        });
    }

    @SubscribeEvent
    public static void loaderRenders(EntityRenderersEvent.AddLayers event) {
        CuriosRendererRegistry.load();
    }
}
