package top.theillusivec4.curios.common.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

import java.util.function.Supplier;

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
