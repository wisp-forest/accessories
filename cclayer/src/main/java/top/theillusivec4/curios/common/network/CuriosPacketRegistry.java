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
        this.registrar = event.registrar(Accessories.MODID);

        registrar.playToServer(CPacketDestroy.TYPE, StreamCodec.unit(new CPacketDestroy()), (arg, iPayloadContext) -> {
            new NukeAccessories().handle(iPayloadContext.player());
        });

        registrar.playToServer(CPacketToggleRender.TYPE, CPacketToggleRender.STREAM_CODEC, (arg, iPayloadContext) -> {
            new SyncCosmeticToggle(null, SlotTypeLoader.INSTANCE.getSlotTypes(true).get(arg.identifier()).name(), arg.index())
                    .handle(iPayloadContext.player());
        });
    }

}
