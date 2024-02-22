package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.AccessoriesInternalsClient;
import io.wispforest.accessories.compat.AccessoriesConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccessoriesClient {
    public static final Event<WindowResizeCallback> WINDOW_RESIZE_CALLBACK_EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) {
            callback.onResized(client, window);
        }
    });
    public static boolean IS_PLAYER_INVISIBLE = false;

    public static void init(){
        AccessoriesInternalsClient.registerToMenuTypes();

        var registry = AutoConfig.getGuiRegistry(AccessoriesConfig.class);
//
//        registry.registerTypeProvider(new GuiProvider() {
//            ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();
//
//            public List<AbstractConfigListEntry> get(String i18n, Field field, Object config, Object defaults, GuiRegistryAccess guiProvider) {
//                var list = Collections.<AbstractConfigListEntry>singletonList(
//                        ENTRY_BUILDER.
//                        ENTRY_BUILDER.startIntList(Component.translatable(i18n), (List) Utils.getUnsafely(field, config))
//                                .setDefaultValue(() -> (List)Utils.getUnsafely(field, defaults))
//                                .setSaveConsumer(newValue -> Utils.setUnsafely(field, config, newValue))
//                                .build()
//                );
//
//                return list;
//            }
//        }, Map.class);
    }

    public interface WindowResizeCallback {

        /**
         * Called after the client's window has been resized
         *
         * @param client The currently active client
         * @param window The window which was resized
         */
        void onResized(Minecraft client, Window window);

    }
}