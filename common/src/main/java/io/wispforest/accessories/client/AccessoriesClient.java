package io.wispforest.accessories.client;

import io.wispforest.accessories.AccessoriesAccessClient;
import org.joml.Vector4i;

public class AccessoriesClient {
    public static boolean renderingPlayerModelInAccessoriesScreen = false;
    public static Vector4i scissorBox = new Vector4i();
    public static String currentSlot = null;
    public static boolean preventSettingShaderColor;

    public static void init(){
        AccessoriesAccessClient.registerToMenuTypes();
    }
}