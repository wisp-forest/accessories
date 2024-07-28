package io.wispforest.cclayer.client;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.cclayer.CCLayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.type.util.IIconHelper;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

@Mod.EventBusSubscriber(modid = CCLayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLayerClient {

    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event){
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

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
