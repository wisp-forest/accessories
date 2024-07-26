package top.theillusivec4.curios.common.network.client;

import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.base.Type;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

// TODO: Why is this not a wrapped packet?
public class CPacketToggleCosmetics  {

    private final int windowId;

    public CPacketToggleCosmetics(int windowId) {
        this.windowId = windowId;
    }

    public static void encode(CPacketToggleCosmetics msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.windowId);
    }

    public static CPacketToggleCosmetics decode(FriendlyByteBuf buf) {
        return new CPacketToggleCosmetics(buf.readInt());
    }

    public static void handle(CPacketToggleCosmetics msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();

            if (sender != null) {
                AbstractContainerMenu container = sender.containerMenu;

                if (container instanceof CuriosContainerV2 && container.containerId == msg.windowId) {
                    ((CuriosContainerV2) container).toggleCosmetics();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
