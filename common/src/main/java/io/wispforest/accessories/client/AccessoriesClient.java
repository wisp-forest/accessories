package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.AccessoriesAccessClient;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;

public class AccessoriesClient {
    public static final Event<WindowResizeCallback> WINDOW_RESIZE_CALLBACK_EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) {
            callback.onResized(client, window);
        }
    });
    public static boolean IS_PLAYER_INVISIBLE = false;

    public static void init(){
        AccessoriesAccessClient.registerToMenuTypes();
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