package io.wispforest.accessories.api.client.screen;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.compat.config.MenuButtonInjection;
import io.wispforest.accessories.mixin.HorseInventoryMenuAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.ContainerClose;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ApiStatus.Experimental
public class AccessoriesScreenTransitionHelper {

    private static boolean INITIALIZED_INJECTIONS = false;

    public static final Event<MenuButtonInjectionCallback> EVENT = EventFactory.createArrayBacked(MenuButtonInjectionCallback.class, invokers -> registerFunc -> {
        for (var invoker : invokers) invoker.registerInjections(registerFunc);
    });

    private static final List<Class<AbstractContainerScreen<AbstractContainerMenu>>> SCREEN_CLASSES = new ArrayList<>();

    private static final Map<ResourceLocation, MenuButtonInjection> BUTTON_INJECTION_DATA = new HashMap<>();

    private static final Map<Predicate<AbstractContainerScreen<AbstractContainerMenu>>, ScreenTransitionHelper> SCREEN_PREDICATES = new LinkedHashMap<>();

    private static final List<PlayerBasedTargetGetter> SCREENLESS_TARGET_GETTERS = new ArrayList<>();

    private static final List<ScreenOpener> SCREEN_OPENERS = new ArrayList<>();

    public static void registerTargetGetter(PlayerBasedTargetGetter getter) {
        SCREENLESS_TARGET_GETTERS.add(getter);
    }

    public static void registerScreenOpener(ScreenOpener screenOpener) {
        SCREEN_OPENERS.add(screenOpener);
    }

    @SafeVarargs
    public static void registerScreensForButton(Class<? extends AbstractContainerScreen<?>>... screenClasses) {
        SCREEN_CLASSES.addAll((Collection) List.of(screenClasses));
    }

    public static <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerPlayerScreenTransition(ResourceLocation location, Class<S> screenClass) {
        registerPlayerScreenTransition(location, screenClass::isInstance);

        registerScreensForButton(screenClass);
    }

    public static void registerPlayerScreenTransition(ResourceLocation location, Predicate<AbstractContainerScreen<AbstractContainerMenu>> screenPredicate) {
        registerScreenTransition(location, screenPredicate, ScreenBasedTargetGetter.PLAYER_DEFAULTED_TARGET, ScreenReopener.PLAYER_INVENTORY);
    }

    public static <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreenTransitionWithCustomInvReopener(ResourceLocation location, Class<S> screenClass, ScreenBasedTargetGetter<M, S> getter) {
        registerScreenTransition(location, screenClass, getter, (ScreenReopener<M, S>) ScreenReopener.CUSTOM_INVENTORY);
    }

    public static <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreenTransition(ResourceLocation location, Class<S> screenClass, ScreenBasedTargetGetter<M, S> getter, ScreenReopener<M, S> reopener) {
        registerScreenTransition(location,
                screenClass::isInstance,
                (ScreenBasedTargetGetter<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>>) getter,
                (ScreenReopener<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>>) reopener);

        registerScreensForButton(screenClass);
    }

    public static void registerScreenTransition(ResourceLocation location, Predicate<AbstractContainerScreen<AbstractContainerMenu>> screenPredicate, ScreenBasedTargetGetter<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> targetEntityGetter, ScreenReopener<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> reopener) {
        SCREEN_PREDICATES.put(screenPredicate, new ScreenTransitionHelper(location, targetEntityGetter, reopener));
    }

    //--

    @ApiStatus.Internal
    public static Class<AbstractContainerScreen>[] getScreenClasses() {
        return List.copyOf(SCREEN_CLASSES).toArray(value -> new Class[value]);
    }

    private static @Nullable Screen prevScreen = null;
    private static @Nullable MenuButtonInjection prevInjection = null;
    private static @Nullable AccessoriesScreenTransitionHelper.ScreenTransitionHelper prevScreenInfo = null;

    @ApiStatus.Internal
    public static @Nullable MenuButtonInjection getInjection(AbstractContainerScreen<AbstractContainerMenu> screen) {
        if (!INITIALIZED_INJECTIONS) initInjections();

        if (screen.equals(prevScreen)) return prevInjection;

        MenuButtonInjection injection = null;
        ScreenTransitionHelper info = null;

        try {
            var typeLocation = BuiltInRegistries.MENU.getKey(screen.getMenu().getType());

            injection = BUTTON_INJECTION_DATA.get(typeLocation);
        } catch (Exception ignored) {}

        if (injection == null) {
            info = getInfo(screen);

            if(info != null) {
                injection = BUTTON_INJECTION_DATA.get(info.location());
            }
        }

        prevScreen = screen;
        prevInjection = injection;
        prevScreenInfo = info;

        return injection;
    }

    private static @Nullable AccessoriesScreenTransitionHelper.ScreenTransitionHelper getInfo(AbstractContainerScreen<AbstractContainerMenu> screen) {
        for (var entry : SCREEN_PREDICATES.entrySet()) {
            if (entry.getKey().test(screen)) return entry.getValue();
        }

        return null;
    }

    @ApiStatus.Internal
    @Nullable
    public static LivingEntity getTargetEntity(AbstractContainerScreen<AbstractContainerMenu> screen) {
        if (prevScreenInfo == null) return null;

        return prevScreenInfo.getter().getTarget(screen);
    }

    @ApiStatus.Internal
    @Nullable
    public static LivingEntity getTargetEntity(LocalPlayer player) {
        for (var targetGetter : SCREENLESS_TARGET_GETTERS) {
            var target = targetGetter.getTarget(player);

            if (target != null) return target;
        }

        return null;
    }

    @ApiStatus.Internal
    public static void openPrevScreen(Player player, LivingEntity targetEntity, @Nullable AbstractContainerScreen<AbstractContainerMenu> screen) {
        if (screen != null) {
            var info = getInfo(screen);

            if (info != null) {
                if(info.reopener.reopenScreen(player, targetEntity, screen)) return;
            }
        } else {
            if (Accessories.config().screenOptions.backButtonClosesScreen()) {
                Minecraft.getInstance().setScreen(null);

                return;
            }

            for (var screenOpener : SCREEN_OPENERS) {
                if (screenOpener.openScreen(player, targetEntity)) return;
            }
        }

        Minecraft.getInstance().setScreen(new InventoryScreen(player));

        player.containerMenu = player.inventoryMenu;

        AccessoriesNetworking.sendToServer(new ContainerClose());
    }

    @ApiStatus.Internal
    public static void init() {
        registerPlayerScreenTransition(ResourceLocation.withDefaultNamespace("creative_player_inventory"), CreativeModeInventoryScreen.class);
        registerPlayerScreenTransition(ResourceLocation.withDefaultNamespace("player_inventory"), InventoryScreen.class);

        //--

        registerScreenTransitionWithCustomInvReopener(
                ResourceLocation.withDefaultNamespace("horse_inventory"),
                HorseInventoryScreen.class,
                (HorseInventoryScreen screen) -> ((HorseInventoryMenuAccessor) screen.getMenu()).accessories$horse()
        );

        registerTargetGetter(player -> {
            return (player.getVehicle() instanceof AbstractHorse abstractHorse)
                    ? abstractHorse
                    : null;
        });

        registerScreenOpener(ScreenOpener.CUSTOM_INVENTORY);
    }

    public static void initInjections() {
        Consumer<List<MenuButtonInjection>> consumer = injections -> {
            BUTTON_INJECTION_DATA.clear();

            injections.forEach(injection -> BUTTON_INJECTION_DATA.putIfAbsent(injection.menuType(), injection));

            EVENT.invoker().registerInjections(BUTTON_INJECTION_DATA::putIfAbsent);
        };

        Accessories.config().screenOptions.subscribeToMenuButtonInjections(consumer);

        consumer.accept(Accessories.config().screenOptions.menuButtonInjections());

        INITIALIZED_INJECTIONS = true;
    }

    private record ScreenTransitionHelper(ResourceLocation location, ScreenBasedTargetGetter<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> getter, ScreenReopener<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> reopener) { }

    public interface MenuButtonInjectionCallback {
        void registerInjections(BiConsumer<ResourceLocation, MenuButtonInjection> registerFunc);
    }
}
