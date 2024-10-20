package top.theillusivec4.curios.common.network;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.common.network.client.CPacketDestroy;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;

import java.util.function.Consumer;

public class CuriosPacketRegistry {

    public static final CuriosPacketRegistry INSTANCE = new CuriosPacketRegistry();

    private CuriosPacketRegistry() {};

    @Nullable
    private PayloadRegistrar registrar = null;

    public void initializeNetworking(final RegisterPayloadHandlersEvent event) {
        this.registrar = event.registrar("cclayer");

        registrar.playToServer(CPacketDestroy.TYPE, StreamCodec.unit(new CPacketDestroy()), (arg, iPayloadContext) -> {
            NukeAccessories.handlePacket(new NukeAccessories(), iPayloadContext.player());
        });

        registrar.playToServer(CPacketToggleRender.TYPE, CPacketToggleRender.STREAM_CODEC, (arg, iPayloadContext) -> {
            var packet = new SyncCosmeticToggle(null, SlotTypeLoader.INSTANCE.getSlotTypes(true).get(arg.identifier()).name(), arg.index());
            SyncCosmeticToggle.handlePacket(packet, iPayloadContext.player());
        });
    }

}
