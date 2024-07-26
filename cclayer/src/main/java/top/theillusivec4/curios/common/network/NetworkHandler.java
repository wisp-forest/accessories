package top.theillusivec4.curios.common.network;

import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    public static SimpleChannel INSTANCE = new DeferredSimpleChannel();
}
