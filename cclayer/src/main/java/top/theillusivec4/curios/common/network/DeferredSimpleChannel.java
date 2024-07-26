package top.theillusivec4.curios.common.network;

import io.wispforest.accessories.neoforge.AccessoriesForgeNetworkHandler;
import io.wispforest.cclayer.WrappedAccessoriesPacket;
import io.wispforest.cclayer.mixin.NetworkInstanceAccessor;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class DeferredSimpleChannel extends SimpleChannel {

    public DeferredSimpleChannel() {
        super(NetworkInstanceAccessor.createNetworkInstance(new ResourceLocation("nope", "nope"), () -> "nope", (v) -> true, (v) -> true));
    }

    @Override
    public <MSG> void sendToServer(MSG message) {
        if(!(message instanceof WrappedAccessoriesPacket wrappedPacket)) {
            throw new IllegalStateException("Unable to handle packet for Curios Internal DeferredSimpleChannel! [Class: " + message.getClass().getSimpleName() + "]");
        }

        AccessoriesForgeNetworkHandler.INSTANCE.sendToServer(wrappedPacket.packet);
    }

    @Override
    public <MSG> void sendTo(MSG message, Connection manager, NetworkDirection direction) {
        if(!(message instanceof WrappedAccessoriesPacket wrappedPacket)) {
            throw new IllegalStateException("Unable to handle packet for Curios Internal DeferredSimpleChannel! [Class: " + message.getClass().getSimpleName() + "]");
        }

        AccessoriesForgeNetworkHandler.INSTANCE.sendWithConnection(manager, direction, wrappedPacket.packet);
    }

    @Override
    public <MSG> void send(PacketDistributor.PacketTarget target, MSG message) {
        if(!(message instanceof WrappedAccessoriesPacket wrappedPacket)) {
            throw new IllegalStateException("Unable to handle packet for Curios Internal DeferredSimpleChannel! [Class: " + message.getClass().getSimpleName() + "]");
        }

        AccessoriesForgeNetworkHandler.INSTANCE.sendWithDistributor(target, wrappedPacket.packet);
    }
}
