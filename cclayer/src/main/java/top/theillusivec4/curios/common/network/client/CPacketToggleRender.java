package top.theillusivec4.curios.common.network.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.CuriosConstants;

public record CPacketToggleRender(String identifier, int index) implements CustomPacketPayload {

    public static final Type<CPacketToggleRender> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, "toggle_render"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketToggleRender> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, CPacketToggleRender::identifier,
                    ByteBufCodecs.INT, CPacketToggleRender::index, CPacketToggleRender::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    //

}
