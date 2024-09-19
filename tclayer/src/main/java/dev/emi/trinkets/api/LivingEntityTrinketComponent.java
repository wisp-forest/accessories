package dev.emi.trinkets.api;

import dev.emi.trinkets.compat.WrappedTrinketComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class LivingEntityTrinketComponent extends WrappedTrinketComponent implements AutoSyncedComponent {

    public LivingEntityTrinketComponent(LivingEntity entity) {
        super(entity);
    }

    //--

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return false;
    }
}