package io.wispforest.cclayer.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.cclayer.CCLayer;

import io.wispforest.cclayer.utils.lang.TranslationInjectionEvent;
import net.minecraft.client.Minecraft;
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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = CCLayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLayerClient {

    private static final BiFunction<Map<String, String>, String, String> BASE_KEY_CONVERTER = (translations, slot) -> {
        return translations.get("accessories.tooltip.attributes.slot")
                .replace("%s", translations.get("accessories.slot." + slot));
    };

    private static final Map<String, TranslationInjectionEvent.Redirection> CURIOS_TO_ACCESSORIES_TRANSLATIONS = Map.ofEntries(
            Map.entry("curios.identifier.curio", map -> map.get("accessories.slot.any")),
            Map.entry("curios.identifier.necklace", map -> map.get("accessories.slot.necklace")),
            Map.entry("curios.identifier.ring", map -> map.get("accessories.slot.ring")),
            Map.entry("curios.identifier.head", map -> map.get("accessories.slot.hat")),
            Map.entry("curios.identifier.back", map -> map.get("accessories.slot.back")),
            Map.entry("curios.identifier.belt", map -> map.get("accessories.slot.belt")),
            Map.entry("curios.identifier.body", map -> map.get("accessories.slot.cape")),
            Map.entry("curios.identifier.charm", map -> map.get("accessories.slot.charm")),
            Map.entry("curios.identifier.hands", map -> map.get("accessories.slot.hand")),
            Map.entry("curios.identifier.bracelet", map -> map.get("accessories.slot.wrist")),
            Map.entry("curios.modifiers.curio", map -> map.get("accessories.tooltip.attributes.any")),
            Map.entry("curios.modifiers.necklace", map -> BASE_KEY_CONVERTER.apply(map, "necklace")),
            Map.entry("curios.modifiers.ring", map -> BASE_KEY_CONVERTER.apply(map, "ring")),
            Map.entry("curios.modifiers.head", map -> BASE_KEY_CONVERTER.apply(map, "hat")),
            Map.entry("curios.modifiers.back", map -> BASE_KEY_CONVERTER.apply(map, "back")),
            Map.entry("curios.modifiers.belt", map -> BASE_KEY_CONVERTER.apply(map, "belt")),
            Map.entry("curios.modifiers.body", map -> BASE_KEY_CONVERTER.apply(map, "cape")),
            Map.entry("curios.modifiers.charm", map -> BASE_KEY_CONVERTER.apply(map, "charm")),
            Map.entry("curios.modifiers.hands", map -> BASE_KEY_CONVERTER.apply(map, "hand")),
            Map.entry("curios.modifiers.bracelet", map -> BASE_KEY_CONVERTER.apply(map, "wrist"))
    );

    @SubscribeEvent
    public static void onClientInit(FMLClientSetupEvent event){
        CCLayer.clientLevelSupplier = () -> Minecraft.getInstance().level;

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

        TranslationInjectionEvent.AFTER_LANGUAGE_LOAD.register(helper -> helper.addRedirections(CURIOS_TO_ACCESSORIES_TRANSLATIONS));
    }

    @SubscribeEvent
    public static void loaderRenders(EntityRenderersEvent.AddLayers event) {
        CuriosRendererRegistry.load();
    }
}
