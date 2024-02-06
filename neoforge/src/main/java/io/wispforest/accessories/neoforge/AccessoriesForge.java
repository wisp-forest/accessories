package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.InstanceCodecable;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(Accessories.MODID)
public class AccessoriesForge {

    public static final AttachmentType<AccessoriesHolder> HOLDER_ATTACHMENT_TYPE;

    public static final EntityCapability<AccessoriesCapability, Void> CAPABILITY = EntityCapability.createVoid(Accessories.of("capability"), AccessoriesCapability.class);

    static {
        HOLDER_ATTACHMENT_TYPE = Registry.register(
                NeoForgeRegistries.ATTACHMENT_TYPES,
                Accessories.of("inventory_holder"),
                AttachmentType.<AccessoriesHolder>builder(AccessoriesHolderImpl::new)
                        .serialize(InstanceCodecable.constructed(AccessoriesHolderImpl::new))
                        .copyOnDeath()
                        .build()
        );
    }

    public AccessoriesForge() {
        Accessories.init();
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event){

    }
}
