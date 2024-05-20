package top.theillusivec4.curios.common.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.CuriosConstants;

import javax.annotation.Nonnull;

public record CPacketToggleCosmetics(int windowId) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(CuriosConstants.MOD_ID, "toggle_cosmetics");

    public CPacketToggleCosmetics(final FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    @Override
    public void write(@Nonnull FriendlyByteBuf buf) {
        buf.writeInt(this.windowId());
    }

    @Nonnull
    @Override
    public ResourceLocation id() {
        return ID;
    }
}
