package io.wispforest.accessories.neoforge.client;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.neoforge.AccessoriesForge;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static io.wispforest.accessories.Accessories.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AccessoriesClientForge {

    public static KeyMapping OPEN_SCREEN;

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AccessoriesClient.init();

            NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::clientTick);
            NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::itemTooltipCallback);

            AccessoriesForge.BUS.addListener(AccessoriesClientForge::registerClientReloadListeners);

            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> {
                return new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> AutoConfig.getConfigScreen(AccessoriesConfig.class, parent).get());
            });
        });
    }

    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesRendererRegistry.onReload();
            }
        });
    }

    @SubscribeEvent
    public static void initKeybindings(RegisterKeyMappingsEvent event){
        OPEN_SCREEN = new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories");

        event.register(OPEN_SCREEN);
    }

    public static void clientTick(TickEvent.ClientTickEvent event){
        if (OPEN_SCREEN.consumeClick()){
            AccessoriesClient.attemptToOpenScreen(Minecraft.getInstance().player.isShiftKeyDown());
        }
    }

    public static void itemTooltipCallback(ItemTooltipEvent event){
        var player = event.getEntity();

        if(player == null) return;

        var stackTooltip = event.getToolTip();

        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.addTooltipInfo(player, event.getItemStack(), stackTooltip);

        stackTooltip.addAll(1, tooltipData);
    }

//    public static void addRenderLayer(EntityRenderersEvent.AddLayers event){
//        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
//            try {
//                var renderer = event.getRenderer((EntityType<? extends LivingEntity>) entityType);
//
//                if(renderer instanceof LivingEntityRenderer<?,?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel){
//                    livingEntityRenderer.addLayer(new AccessoriesRenderLayer<>(livingEntityRenderer));
//                }
//            } catch (ClassCastException ignore){}
//        }
//    }
}