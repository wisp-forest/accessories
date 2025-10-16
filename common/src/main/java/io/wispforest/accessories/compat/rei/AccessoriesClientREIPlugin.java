package io.wispforest.accessories.compat.rei;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.client.gui.components.ArrowComponent;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.mixin.client.AbstractContainerScreenAccessor;
import io.wispforest.owo.ui.component.ButtonComponent;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.plugin.autocrafting.InventoryCraftingTransferHandler;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AccessoriesClientREIPlugin implements REIClientPlugin {
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(AccessoriesScreen.class, screen -> {
            return screen.getComponentRectangles().stream()
                    .map(rectangle -> new Rectangle(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()))
                    .toList();
        });
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerContainerClickArea(
                screen -> {
                    var accessor = ((AbstractContainerScreenAccessor) (screen));

                    if(screen.getDefaultedData(PlayerOptions.SHOW_CRAFTING_GRID)) {
                        var component = screen.component(ArrowComponent.class, "crafting_arrow");

                        if (component != null) {
                            return new Rectangle(component.x() - accessor.accessories$leftPos(), component.y() - accessor.accessories$topPos(), component.width(), component.height());
                        }
                    }

                    return new Rectangle(0, 0, 0, 0);
                },
                AccessoriesScreen.class,
                BuiltinPlugin.CRAFTING);
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        var transferHandler = new SimpleTransferHandler() {
            private final CategoryIdentifier<?> categoryIdentifier = BuiltinPlugin.CRAFTING;
            private final IntRange inputSlots = new SimpleTransferHandler.IntRange(1, 5);

            @Override
            public ApplicabilityResult checkApplicable(Context context) {
                if (categoryIdentifier.equals(context.getDisplay().getCategoryIdentifier())
                        && context.getContainerScreen() instanceof AccessoriesScreen screen) {

                    if (!screen.getDefaultedData(PlayerOptions.SHOW_CRAFTING_GRID) && context.isActuallyCrafting()) {
                        var component = screen.component(ButtonComponent.class, "crafting_grid_btn");

                        component.onPress(new MouseButtonEvent(0,0, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_1,0)));
                    }

                    return ApplicabilityResult.createApplicable();
                }

                return ApplicabilityResult.createNotApplicable();
            }

            @Override
            public Iterable<SlotAccessor> getInputSlots(Context context) {
                return IntStream.range(inputSlots.min(), inputSlots.maxExclusive())
                        .mapToObj(id -> SlotAccessor.fromSlot(context.getMenu().getSlot(id)))
                        .toList();
            }

            @Override
            public Iterable<SlotAccessor> getInventorySlots(Context context) {
                var player = context.getMinecraft().player;

                return IntStream.range(0, player.getInventory().getNonEquipmentItems().size())
                        .mapToObj(index -> SlotAccessor.fromPlayerInventory(player, index))
                        .collect(Collectors.toList());
            }
        };

        registry.register(new InventoryCraftingTransferHandler(transferHandler));
    }
}
