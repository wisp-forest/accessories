package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.InstanceCodecable;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;

public class AccessoriesModFabric implements ModInitializer {

    public static final AttachmentType<AccessoriesHolder> HOLDER_ATTACHMENT_TYPE;

    static {
        HOLDER_ATTACHMENT_TYPE = AttachmentRegistry.<AccessoriesHolder>builder()
                .initializer(AccessoriesHolderImpl::new)
                .persistent(InstanceCodecable.constructed(AccessoriesHolderImpl::new))
                .copyOnDeath()
                .buildAndRegister(Accessories.of("INVENTORY_HOLDER"));
    }

    @Override
    public void onInitialize() {
        Accessories.init();

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // TODO: DROP ACCESSORIES!
        });

        //ServerPlayConnectionEvents.JOIN
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            // TODO: SYNC DATA TO JOINING CLIENT and handled reload!?
        });
    }
}
