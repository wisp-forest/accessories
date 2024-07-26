package top.theillusivec4.curios.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

import java.util.function.Supplier;

public class CuriosRegistry {

    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CuriosApi.MODID);

    public static final RegistryObject<MenuType<CuriosContainer>> CURIO_MENU = MENU_TYPES.register("curios_container", () -> IForgeMenuType.create(CuriosContainer::new));
    public static final RegistryObject<MenuType<CuriosContainerV2>> CURIO_MENU_NEW = MENU_TYPES.register("curios_container_v2", () -> IForgeMenuType.create(CuriosContainerV2::new));

    public static void init() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MENU_TYPES.register(eventBus);
    }
}
