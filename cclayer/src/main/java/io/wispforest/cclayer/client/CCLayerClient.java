package io.wispforest.cclayer.client;

import io.wispforest.cclayer.CCLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ClientNeoForgeMod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = CCLayer.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CCLayerClient {

    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event){
        var bus = ModLoadingContext.get().getActiveContainer().getEventBus();

        bus.addListener(CCLayerClient::loaderRenders);
    }

    @SubscribeEvent
    public static void loaderRenders(EntityRenderersEvent.AddLayers event) {
        CuriosRendererRegistry.load();
    }
}
