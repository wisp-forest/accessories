package top.theillusivec4.curios.common;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.capability.CurioInventory;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

import java.util.function.Supplier;

public class CuriosRegistry {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CuriosApi.MODID);

    public static final Supplier<AttachmentType<CurioInventory>> INVENTORY = ATTACHMENT_TYPES.register("inventory", () -> AttachmentType.serializable(CurioInventory::new).copyOnDeath().build());

    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CuriosApi.MODID);

    public static final Supplier<MenuType<CuriosContainer>> CURIO_MENU = MENU_TYPES.register("curios_container", () -> IMenuTypeExtension.create(CuriosContainer::new));
    public static final Supplier<MenuType<CuriosContainerV2>> CURIO_MENU_NEW = MENU_TYPES.register("curios_container_v2", () -> IMenuTypeExtension.create(CuriosContainerV2::new));

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CuriosApi.MODID);

    public static final Supplier<DataComponentType<CurioAttributeModifiers>>
            CURIO_ATTRIBUTE_MODIFIERS = DATA_COMPONENTS.register("attribute_modifiers",
            () -> DataComponentType.<CurioAttributeModifiers>builder()
                    .persistent(CurioAttributeModifiers.CODEC)
                    .networkSynchronized(CurioAttributeModifiers.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static void init(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
        MENU_TYPES.register(eventBus);
        DATA_COMPONENTS.register(eventBus);
    }
}
