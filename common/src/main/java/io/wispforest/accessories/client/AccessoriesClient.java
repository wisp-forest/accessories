package io.wispforest.accessories.client;

import io.wispforest.accessories.AccessoriesAccessClient;

public class AccessoriesClient {
    public static boolean IS_PLAYER_INVISIBLE = false;

    public static void init(){
        AccessoriesAccessClient.registerToMenuTypes();
    }
}