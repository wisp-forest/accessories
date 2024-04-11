package top.theillusivec4.curios.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.capability.CurioInventory;

import java.util.function.Supplier;

public class CuriosRegistry {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CuriosApi.MODID);

    public static final Supplier<AttachmentType<CurioInventory>> INVENTORY = ATTACHMENT_TYPES.register(
            "inventory",
            () -> AttachmentType.serializable(CurioInventory::new).copyOnDeath().build());

    public static void init(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
