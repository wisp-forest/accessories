package top.theillusivec4.curios.common.network.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.CuriosConstants;

public record CPacketDestroy() implements CustomPacketPayload {

    public static final Type<CPacketDestroy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, "destroy"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
