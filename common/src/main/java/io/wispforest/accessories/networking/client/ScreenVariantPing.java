package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.gui.ScreenVariantSelectionScreen;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record ScreenVariantPing(int entityId, boolean targetLookEntity) implements HandledPacketPayload {

    public static final Endec<ScreenVariantPing> ENDEC = StructEndecBuilder.of(
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

    @Override
    public void handle(Player player) {
        var selectedVariant = AccessoriesMenuVariant.getVariant(Accessories.getConfig().clientData.selectedScreenType);

        Function<AccessoriesMenuVariant, ScreenOpen> packetBuilder = (menuVariant) -> {
            return new ScreenOpen(targetLookEntity ? -1 : entityId, targetLookEntity, menuVariant);
        };

        if(selectedVariant != null) {
            AccessoriesInternals.getNetworkHandler().sendToServer(packetBuilder.apply(selectedVariant));
        } else {
            Minecraft.getInstance().setScreen(new ScreenVariantSelectionScreen(variant -> {
                AccessoriesInternals.getNetworkHandler().sendToServer(packetBuilder.apply(variant));
            }));
        }
    }
}
