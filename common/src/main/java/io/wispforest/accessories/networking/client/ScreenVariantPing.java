package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.ScreenVariantSelectionScreen;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record ScreenVariantPing(int entityId, boolean targetLookEntity) {

    public static final StructEndec<ScreenVariantPing> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", ScreenVariantPing::entityId),
            Endec.BOOLEAN.fieldOf("targetLookEntity", ScreenVariantPing::targetLookEntity),
            ScreenVariantPing::new
    );

    public static ScreenVariantPing of(@Nullable LivingEntity livingEntity){
        return new ScreenVariantPing(livingEntity != null ? livingEntity.getId() : -1, false);
    }

    public static ScreenVariantPing of(boolean targetLookEntity){
        return new ScreenVariantPing(-1, targetLookEntity);
    }

    @Environment(EnvType.CLIENT)
    public static void handlePacket(ScreenVariantPing packet, Player player) {
        var selectedVariant = AccessoriesMenuVariant.getVariant(Accessories.config().screenOptions.selectedScreenType());

        Function<AccessoriesMenuVariant, ScreenOpen> packetBuilder = (menuVariant) -> {
            return new ScreenOpen(packet.targetLookEntity() ? -1 : packet.entityId(), packet.targetLookEntity(), menuVariant);
        };

        if(selectedVariant != null) {
            AccessoriesNetworking.sendToServer(packetBuilder.apply(selectedVariant));
        } else {
            Minecraft.getInstance().setScreen(new ScreenVariantSelectionScreen(variant -> {
                AccessoriesNetworking.sendToServer(packetBuilder.apply(variant));
            }));
        }
    }
}
