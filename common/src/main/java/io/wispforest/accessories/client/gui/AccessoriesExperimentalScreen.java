package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.components.*;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.mixin.client.AbstractContainerScreenAccessor;
import io.wispforest.accessories.mixin.client.owo.DiscreteSliderComponentAccessor;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import io.wispforest.owo.mixin.ui.SlotAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static io.wispforest.accessories.client.gui.components.ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE;

public class AccessoriesExperimentalScreen extends BaseOwoHandledScreen<FlowLayout, AccessoriesExperimentalMenu> implements AccessoriesScreenBase, ContainerScreenExtension {

    private static final Logger LOGGER = LogUtils.getLogger();

    public AccessoriesExperimentalScreen(AccessoriesExperimentalMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        this.inventoryLabelX = 42069;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    //--

    public final Map<Integer, Boolean> changedSlots = new HashMap<>();

    public void hideSlot(int index) {
        hideSlot(this.menu.slots.get(index));
    }

    public void hideSlot(Slot slot) {
        ((SlotAccessor) slot).owo$setX(-300);
        ((SlotAccessor) slot).owo$setY(-300);
    }

    @Override
    public void disableSlot(Slot slot) {
        disableSlot(slot.index);
    }

    @Override
    public void disableSlot(int index) {
        super.disableSlot(index);

        var state = this.changedSlots.getOrDefault(index, null);

        if(state != null && !state) return;

        hideSlot(index);

        this.changedSlots.put(index, true);
    }

    @Override
    public void enableSlot(Slot slot) {
        enableSlot(slot.index);
    }

    @Override
    public void enableSlot(int index) {
        super.enableSlot(index);

        var state = this.changedSlots.getOrDefault(index, null);

        if(state != null && state) return;

        this.changedSlots.put(index, false);
    }

    @Override
    protected boolean isSlotEnabled(int index) {
        return isSlotEnabled(this.menu.slots.get(index));
    }

    @Override
    protected boolean isSlotEnabled(Slot slot) {
        return !((OwoSlotExtension) slot).owo$getDisabledOverride();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if(this.changedSlots.isEmpty()) return;

        var slots = this.getMenu().slots;

        var changes = this.changedSlots.keySet().stream()
                .map(i -> (i < slots.size()) ? slots.get(i) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(slot -> slot.index, slot -> ((OwoSlotExtension)slot).owo$getDisabledOverride()));

        this.getMenu().sendMessage(new AccessoriesExperimentalMenu.ToggledSlots(changes));

        this.changedSlots.clear();
    }

    @Override
    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
    }

    //--

    private boolean showCosmeticState = false;

    public void showCosmeticState(boolean value) {
        this.showCosmeticState = value;
    }

    public boolean showCosmeticState() {
        return showCosmeticState;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        changedSlots.clear();

        rootComponent.allowOverflow(true)
                .surface(Surface.VANILLA_TRANSLUCENT);
        //rootComponent.surface(SLOT_RENDERING_SURFACE);

        var baseChildren = new ArrayList<io.wispforest.owo.ui.core.Component>();

        //--

        for (int i = 0; i < this.getMenu().slots.size(); i++) {
            this.disableSlot(i);
        }

        var menu = this.getMenu();

        //--

        baseChildren.add(ComponentUtils.createPlayerInv(menu.startingAccessoriesSlot(), this::slotAsComponent, this::enableSlot));

        //--

        var armorAndEntityLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("armor_entity_layout");

        {
            var armorsLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .gap(68)
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    .positioning(Positioning.relative(50, 50))
                    .zIndex(10);

            var innerSpacingComponent = Containers.horizontalFlow(Sizing.fixed(60), Sizing.fixed(0));

            var outerLeftArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            var outerRightArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            var armorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            armorSlotsLayout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerLeftArmorLayout.child(armorSlotsLayout);

            var cosmeticArmorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            cosmeticArmorSlotsLayout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerRightArmorLayout.child(cosmeticArmorSlotsLayout);

            for (int i = 0; i < menu.addedArmorSlots() / 2; i++) {
                var armor = menu.startingAccessoriesSlot() + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                var armorSlot = this.slotAsComponent(armor)
                        .margins(Insets.of(1));

                var cosmeticArmorSlot = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) this.menu.slots.get(cosmeticArmor), false, this::slotAsComponent).left();

                armorSlotsLayout.child(armorSlot);
                cosmeticArmorSlotsLayout.child(cosmeticArmorSlot);
            }

            //outerArmorLayout.positioning(Positioning.relative(50, 20));

            outerLeftArmorLayout
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .surface(Surface.PANEL)
                    .padding(Insets.of(6));

            outerRightArmorLayout
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .surface(Surface.PANEL)
                    .padding(Insets.of(6));

            armorsLayout.child(outerLeftArmorLayout);

            armorsLayout.child(innerSpacingComponent);
//            ((ExclusiveBoundingArea) armorsLayout).addExclusionZone(innerSpacingComponent);

            armorsLayout.child(outerRightArmorLayout);

            //--

            var entityComponentSize = 126;

            var entityContainer = Containers.stack(Sizing.content(), Sizing.fixed(entityComponentSize + 12))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            InventoryEntityComponent.of(Sizing.fixed(entityComponentSize), Sizing.fixed(108), this.targetEntityDefaulted())
                                                    .startingRotation(this.leftPositioned() ? -45 : 45)
                                                    .scaleToFit(true)
                                                    .allowMouseRotation(true)
                                                    .id("entity_rendering_component")
                                    )
                                    .surface(Surface.flat(Color.BLACK.argb()))
                    )
                    .child(
                            outerLeftArmorLayout.positioning(Positioning.relative(0, 40))
                                    .margins(Insets.left(-6))
                                    .zIndex(10)
                    )
                    .child(
                            outerRightArmorLayout.positioning(Positioning.relative(100, 40))
                                    .margins(Insets.right(-6))
                                    .zIndex(10)
                    )
                    .child(
                            Components.button(Component.literal(""), (btn) -> {
                                this.minecraft.setScreen(new InventoryScreen(minecraft.player));
                            }).renderer((context, btn, delta) -> {
                                        ButtonComponent.Renderer.VANILLA.draw(context, btn, delta);

                                        context.push().translate(0.5, 0.5, 0.0);

                                        var BACK_ICON = Accessories.of("widget/back");

                                        context.blitSprite(BACK_ICON, btn.x(), btn.y(), btn.width() - 1, btn.height() - 1);

                                        context.pop();
                                    })
                                    .positioning(Positioning.relative(100, 0))
                                    .margins(Insets.of(1, 0, 0, 1))
                                    .sizing(Sizing.fixed(8))
                    )
                    .padding(Insets.of(6))
                    .surface(Surface.PANEL);

            var offHandIndex = this.getMenu().startingAccessoriesSlot() - (this.getMenu().includeSaddle() ? 2 : 1);

            this.enableSlot(offHandIndex);

            if(this.getMenu().includeSaddle()) {
                var saddleIndex = this.getMenu().startingAccessoriesSlot() - 1;

                this.enableSlot(saddleIndex);

                ((StackLayout) entityContainer).child(
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .child(
                                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                .child(
                                                        this.slotAsComponent(saddleIndex)
                                                                .margins(Insets.of(1))
                                                )
                                                .surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                ).surface(Surface.PANEL)
                                .padding(Insets.of(6))
                                .positioning(Positioning.relative(0, 100))
                                .margins(Insets.of(0, -6, -6, 0))
                                .zIndex(10)
                        );
            }

            ((StackLayout) entityContainer).child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(
                                Containers.verticalFlow(Sizing.content(), Sizing.content())
                                        .child(
                                                this.slotAsComponent(offHandIndex)
                                                        .margins(Insets.of(1))
                                        )
                                        .surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                        ).surface(Surface.PANEL)
                        .padding(Insets.of(6))
                        .positioning(Positioning.relative(100, 100))
                        .margins(Insets.of(0, -6, 0, -6))
                        .zIndex(10)
            );

            //var entityDraggable = Containers.draggable(Sizing.content(), Sizing.content(), entityContainer);

            armorAndEntityLayout.child(entityContainer); //0,
        }

        baseChildren.add(armorAndEntityLayout);

        var accessoriesComponent = createAccessoriesComponent();

        if(accessoriesComponent != null) armorAndEntityLayout.child((this.leftPositioned() ? 0 : 1), accessoriesComponent); //1,

        //--

        var optionPanel = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .surface(Surface.PANEL)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(5));

        {
            var cosmeticToggleButton = Components.button(Component.literal(""), btn -> {
                        showCosmeticState(!showCosmeticState());

                        btn.tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

                        var component = this.uiAdapter.rootComponent.childById(AccessoriesContainingComponent.class, AccessoriesContainingComponent.defaultID());

                        if(component != null) component.onCosmeticToggle(showCosmeticState());
                    }).renderer((context, button, delta) -> {
                        ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

                        var textureAtlasSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                .apply(!showCosmeticState() ? Accessories.of("gui/slot/cosmetic") : Accessories.of("gui/slot/charm"));

                        var color = (!showCosmeticState() ? Color.WHITE.interpolate(Color.BLACK, 0.3f) : Color.BLACK);

                        var red = color.red();
                        var green = color.green();
                        var blue = color.blue();

                        var shard = RenderStateShard.TRANSLUCENT_TRANSPARENCY;

                        //shard.setupRenderState();

                        if(!showCosmeticState()) {
                            GuiGraphicsUtils.drawWithSpectrum(context, button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, 1f);
                            context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.4f);
                        } else {
                            context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.9f);
                        }

                        //shard.clearRenderState();
                    }).sizing(Sizing.fixed(20))
                    .tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

            /*if(!this.menu.addedSlots.isEmpty()) */optionPanel.child(cosmeticToggleButton);
        }

        //--

        var showAdvancedOptions = this.showAdvancedOptions();

        var initalSizing = Sizing.fixed(0);
        var finalSizing = Sizing.content();

        var advancedOptions = (FlowLayout) Containers.horizontalFlow(showAdvancedOptions ? finalSizing : initalSizing, Sizing.content())
                .gap(3)
                .verticalAlignment(VerticalAlignment.CENTER);

        var animation = advancedOptions.horizontalSizing().animate(1000, Easing.CUBIC, showAdvancedOptions ? initalSizing : finalSizing);

        optionPanel.child(
                Components.button(createToggleTooltip("advanced_options", false, showAdvancedOptions), btn -> {
                    this.showAdvancedOptions(!this.showAdvancedOptions());

                    btn.setMessage(createToggleTooltip("advanced_options", false, this.showAdvancedOptions()));
                    btn.tooltip(createToggleTooltip("advanced_options", true, this.showAdvancedOptions()));

                    animation.reverse();
                }).tooltip(createToggleTooltip("advanced_options", true, showAdvancedOptions))
                        .margins(Insets.left(3))
        );

        optionPanel.child(advancedOptions);

        advancedOptions.child(
                Components.box(Sizing.fixed(1), Sizing.fixed(18))
                        .margins(Insets.left(3))
        );

        advancedOptions.child(
                Components.button(
                                createToggleTooltip("unused_slots", false, this.showUnusedSlots()),
                                btn -> {
                                    AccessoriesInternals.getNetworkHandler()
                                            .sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, this.getMenu().owner(), bl -> !bl));
                                }
                        )
                        .tooltip(createToggleTooltip("unused_slots", true, this.showUnusedSlots()))
                        .horizontalSizing(Sizing.fixed(64))
                        .id("unused_slots_toggle")
        );

        {
            var min = getMinimumColumnAmount();
            var max = 18;

            var step = 1f / (max - min);

            var columnAmountSlider = Components.discreteSlider(Sizing.fixed(45), min, max)
                    .snap(true)
                    .setFromDiscreteValue(this.columnAmount())
                    .scrollStep(step);

            columnAmountSlider.tooltip(Accessories.translation("column_slider.tooltip"));

            columnAmountSlider.id("column_amount_slider");

            columnAmountSlider.onChanged().subscribe(value -> {
                AccessoriesInternals.getNetworkHandler()
                        .sendToServer(SyncHolderChange.of(HolderProperty.COLUMN_AMOUNT_PROP, (int) value));

                this.columnAmount((int) value);

                rebuildAccessoriesComponent();
            });

            advancedOptions.child(columnAmountSlider);
        }

        {
            advancedOptions.child(
                    Components.button(
                            widgetTypeToggleMessage(this.widgetType(), false),
                            btn -> {
                                var newWidget = this.widgetType() + 1;

                                if(newWidget > 2) newWidget = 1;

                                AccessoriesInternals.getNetworkHandler()
                                        .sendToServer(SyncHolderChange.of(HolderProperty.WIDGET_TYPE_PROP, newWidget));

                                this.widgetType(newWidget);

                                updateWidgetTypeToggleButton();
                            }).tooltip(widgetTypeToggleMessage(this.widgetType(), true))
                            .id("widget_type_toggle")
            );
        }

        {
            advancedOptions.child(
                    Components.button(
                            createToggleTooltip("left_handed_accessories", false, this.leftPositioned()),
                            btn -> {
                                AccessoriesInternals.getNetworkHandler()
                                        .sendToServer(SyncHolderChange.of(HolderProperty.LEFT_POSITIONED_PROP, this.menu.owner(), bl -> !bl));

                                this.leftPositioned(!this.leftPositioned());

                                this.uiAdapter.rootComponent.childById(InventoryEntityComponent.class, "entity_rendering_component")
                                        .startingRotation(this.leftPositioned() ? -45 : 45);
                            }).tooltip(createToggleTooltip("left_handed_accessories", true, this.leftPositioned()))
                            .id("left_positioned_toggle")
            );
        }

        baseChildren.addLast(optionPanel);

        //--

        var baseLayout = Containers.verticalFlow(Sizing.content(), Sizing.fill())
                .gap(2)
                .children(baseChildren.reversed());

        baseLayout.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(50, 50));

        //--

        rootComponent.child(baseLayout);
    }

    @Nullable
    private AccessoriesContainingComponent topComponent = null;

    private void rebuildAccessoriesComponent() {
        var columnAmountSlider = this.uiAdapter.rootComponent.childById(DiscreteSliderComponent.class, "column_amount_slider");

        var previousValue = columnAmountSlider.discreteValue();

        var newMinimum = getMinimumColumnAmount();

        ((DiscreteSliderComponentAccessor) columnAmountSlider).accessories$setMin(newMinimum);

        var newValue = Math.max((int) Math.round(previousValue), newMinimum);

        columnAmountSlider.setFromDiscreteValue(newValue);

        ((DiscreteSliderComponentAccessor) columnAmountSlider).accessories$updateMessage();

        this.columnAmount(newValue);

        //--

        var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

        for (var child : List.copyOf(armorAndEntityComp.children())) {
            if (AccessoriesContainingComponent.defaultID().equals(child.id()) && child instanceof AccessoriesContainingComponent accessories) {
                var parent = accessories.parent();

                if (parent != null) {
                    ComponentUtils.recursiveSearch(parent, ExtendedSlotComponent.class, slotComponent -> this.hideSlot(slotComponent.slot()));

                    parent.removeChild(accessories);
                }
            }
        }

        var accessoriesComp = createAccessoriesComponent();

        if (accessoriesComp != null) armorAndEntityComp.child((this.leftPositioned() ? 0 : 1), accessoriesComp);
    }

    private int getMinimumColumnAmount() {
        var widgetType = this.widgetType();

        return switch (widgetType) {
            case 2 -> 1;
            default -> 3;
        };
    }

    @Nullable
    private AccessoriesContainingComponent createAccessoriesComponent() {
        var widgetType = this.widgetType();

        var component = switch (widgetType) {
            case 2 -> ScrollableAccessoriesComponent.createOrNull(this); // 2
            default -> GriddedAccessoriesComponent.createOrNull(this); // 1
        };

        this.topComponent = component;

        return component;
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        var accessories = this.topComponent;

        if(accessories != null) {
            var result = accessories.isHovering_Logical(slot, mouseX, mouseY);

            if(result != null) return result;
        }

        return null;
    }

    @Override
    @Nullable
    public Boolean isHovering_Rendering(Slot slot, double mouseX, double mouseY) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : null;
    }

    @Override
    public @Nullable Boolean shouldRenderSlot(Slot slot) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : null;
    }

    @Override
    public void onHolderChange(String key) {
        switch (key) {
            case "unused_slots" -> updateUnusedSlotToggleButton();
            case "left_positioned" -> updateLeftPositionedToggleButton();
        }
    }

    public void updateUnusedSlotToggleButton() {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "unused_slots_toggle");

        var value = this.showUnusedSlots();

        btn.setMessage(createToggleTooltip("unused_slots", false, value));
        btn.tooltip(createToggleTooltip("unused_slots", true, value));

        this.menu.reopenMenu();
    }

    public void updateLeftPositionedToggleButton() {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "left_positioned_toggle");

        var value = this.leftPositioned();

        btn.setMessage(createToggleTooltip("left_handed_accessories", false, value));
        btn.tooltip(createToggleTooltip("left_handed_accessories", true, value));

        this.rebuildAccessoriesComponent();
    }

    public void updateWidgetTypeToggleButton() {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "widget_type_toggle");

        var value = this.widgetType();

        btn.setMessage(widgetTypeToggleMessage(value, false));
        btn.tooltip(widgetTypeToggleMessage(value, true));

        this.rebuildAccessoriesComponent();
    }

    private static Component widgetTypeToggleMessage(int value, boolean isTooltip) {
        var type = switch (value) {
            case 2 -> "scrollable";
            case 1 -> "paginated";
            default -> "";
        };

        return Accessories.translation("widget_type." + type + (isTooltip ? ".tooltip" : ""));
    }

    private static Component createToggleTooltip(String type, boolean isTooltip, boolean value) {
        return Accessories.translation(type + ".toggle." + (value ? "enabled" : "disabled") + (isTooltip ? ".tooltip" : ""));
    }

    //--

    public ExtendedSlotComponent slotAsComponent(int index) {
        return new ExtendedSlotComponent(this, index);
    }

    public class ExtendedSlotComponent extends SlotComponent {

        private boolean isBatched = false;

        protected final AccessoriesExperimentalScreen screen;
        protected int index;

        protected ExtendedSlotComponent(AccessoriesExperimentalScreen screen, int index) {
            super(index);

            this.screen = screen;
            this.index = index;

            this.didDraw = true;
        }

        public final Slot slot() {
            return this.slot;
        }

        public final int index() {
            return index;
        }

        public final boolean isBatched() {
            return this.isBatched;
        }

        public ExtendedSlotComponent isBatched(boolean value) {
            this.isBatched = value;

            return this;
        }

        @Override
        public void dismount(DismountReason reason) {
            super.dismount(reason);

            if(reason == DismountReason.REMOVED) {
                ((SlotAccessor) slot).owo$setX(-300);
                ((SlotAccessor) slot).owo$setY(-300);
            }
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if(!(slot() instanceof SlotTypeAccessible)) {
                super.draw(context, mouseX, mouseY, partialTicks, delta);

                return;
            }

            this.didDraw = true;

            if (isBatched) return;

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            renderSlot(context);

            renderCosmeticOverlay(context, false);

            renderHover(context, () -> screen.hoveredSlot);
        }

        public void renderSlot(OwoUIDrawContext context) {
            int i = screen.leftPos;
            int j = screen.topPos;

            context.push();
            context.translate((float) i, (float) j, 0);

            screen.forceRenderSlot(context, slot());

            context.pop();
        }

        public void renderCosmeticOverlay(OwoUIDrawContext context, boolean externalBatching) {
            if (!(slot() instanceof SlotTypeAccessible slotTypeAccessible) || !slotTypeAccessible.isCosmeticSlot()) return;

            context.push();
            context.translate(0.0F, 0.0F, 101.0F);

            if(externalBatching) {
                GuiGraphicsUtils.drawRectOutlineWithSpectrumWithoutRecord(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);
            } else {
                GuiGraphicsUtils.drawRectOutlineWithSpectrum(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);
            }

            context.pop();
        }

        public void renderHover(OwoUIDrawContext context, Supplier<Slot> hoverSlot) {
            var hoveredSlot = hoverSlot.get();

            if (this.slot.equals(hoveredSlot) && hoveredSlot.isHighlightable()) {
                context.push();

                context.fillGradient(RenderType.gui(), x(), y(), x() + 16, y() + 16, -2130706433, -2130706433, 330);

                context.pop();
            }
        }
    }

    private final Surface SLOT_RENDERING_SURFACE = (context, component) -> {
        var validComponents = new ArrayList<ExtendedSlotComponent>();

        ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
            if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

            validComponents.add(slotComponent);
        });

        context.push();

        {
            /*
            safeBatching(context, hasDrawCallOccur -> {
                for (var slotComponent : validComponents) {
                    slotComponent.renderSlot(context);

                    hasDrawCallOccur.setValue(true);
                }
            });
            */

            context.push();
            context.translate(0, 0, 100.0F);

            //--

            Map<Slot, Triplet<ItemStack, Boolean, @Nullable String>> slotStateData = new HashMap<>();
            Map<Slot, Boolean> allBl2s = new HashMap<>();

            //--

            var accessor = (AbstractContainerScreenAccessor) this;

            //--

            int i = this.leftPos;
            int j = this.topPos;

            context.push();
            context.translate(i, j, 0);

            safeBatching(context, hasDrawCallOccur -> {
                for (var slotComponent : validComponents) {
                    var slot = slotComponent.slot();

                    var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                    if (data == null) return;

                    var itemStack = data.getA();

                    if (itemStack.isEmpty() /*&& slot.isActive()*/) {
                        var pair = slot.getNoItemIcon();

                        if (pair != null) {
                            var textureAtlasSprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());

                            context.blit(slot.x, slot.y, 0, 16, 16, textureAtlasSprite);

                            allBl2s.put(slot, true);
                        }
                    }

                    hasDrawCallOccur.setValue(true);
                }
            });

            //--

            //if(true) return;

            safeBatching(context, hasDrawCallOccur -> {
                for (var slotComponent : validComponents) {
                    var slot = slotComponent.slot();

                    var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                    if (data == null) return;

                    var itemStack = data.getA();

                    var bl2 = allBl2s.getOrDefault(slot, false)
                            || (slot == accessor.accessories$getClickedSlot() && !accessor.accessories$getDraggingItem().isEmpty() && !accessor.accessories$isSplittingStack());

                    if (!bl2) {
                        int slotX = slot.x;
                        int slotY = slot.y;

                        if (data.getB()) {
                            context.fill(slotX, slotY, slotX + 16, slotY + 16, -2130706433);
                        }

                        int k = slot.x + slot.y * this.imageWidth;

                        if (slot.isFake()) {
                            context.renderFakeItem(itemStack, slotX, slotY, k);
                        } else {
                            context.renderItem(itemStack, slotX, slotY, k);
                        }

                        context.renderItemDecorations(this.font, itemStack, slotX, slotY, data.getC());
                    }

                    hasDrawCallOccur.setValue(true);
                }
            });

            context.pop();

            //--

            context.pop();

        }

        safeBatching(context, hasDrawCallOccur -> {
            for (var slotComponent : validComponents) {
                slotComponent.renderCosmeticOverlay(context, true);

                hasDrawCallOccur.setValue(true);
            }
        });

        for (var slotComponent : validComponents) {
            slotComponent.renderHover(context, () -> this.hoveredSlot);
        }

        context.pop();
    };

    public final Surface FULL_SLOT_RENDERING = BACKGROUND_SLOT_RENDERING_SURFACE.and(SLOT_RENDERING_SURFACE);

    //--

    @Nullable
    public Triplet<ItemStack, Boolean, @Nullable String> getRenderStack(Slot slot) {
        var accessor = (AbstractContainerScreenAccessor) this;

        ItemStack itemStack = slot.getItem();

        boolean bl = false;

        ItemStack itemStack2 = this.menu.getCarried();

        String string = null;

        if (slot == accessor.accessories$getClickedSlot() && !accessor.accessories$getDraggingItem().isEmpty() && accessor.accessories$isSplittingStack() && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemStack2.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) return null;

            if (AbstractContainerMenu.canItemQuickReplace(slot, itemStack2, true) && this.menu.canDragTo(slot)) {
                bl = true;

                int maxSlotStackSize = Math.min(itemStack2.getMaxStackSize(), slot.getMaxStackSize(itemStack2));
                int currentSlotStackSize = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();

                int newStackSize = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, accessor.accessories$getQuickCraftingType(), itemStack2) + currentSlotStackSize;

                if (newStackSize > maxSlotStackSize) {
                    newStackSize = maxSlotStackSize;
                    string = ChatFormatting.YELLOW.toString() + maxSlotStackSize;
                }

                itemStack = itemStack2.copyWithCount(newStackSize);
            } else {
                this.quickCraftSlots.remove(slot);
                accessor.accessories$recalculateQuickCraftRemaining();
            }
        }

        return new Triplet<>(itemStack, bl, string);
    }

    //--

    private <T> T getHolderValue(Function<AccessoriesHolder, T> getter, T defaultValue, String valueType) {
        return Optional.ofNullable(AccessoriesHolder.get(this.menu.owner()))
                .map(getter)
                .orElseGet(() -> {
                    LOGGER.warn("[AccessoriesScreen] Unable to get the given holder value '{}' for the given owner: {}", valueType, this.menu.owner().getName());

                    return defaultValue;
                });
    }

    private <T> void setHolderValue(BiFunction<AccessoriesHolder, T, AccessoriesHolder> setter, T value, String valueType) {
        var holder = AccessoriesHolder.get(this.menu.owner());

        if(holder == null) {
            LOGGER.warn("[AccessoriesScreen] Unable to set the given holder value '{}' for the given owner: {}", valueType, this.menu.owner().getName());

            return;
        }

        setter.apply(holder, value);
    }

    private int widgetType() {
        return this.getHolderValue(AccessoriesHolder::widgetType, 1, "widgetType");
    }

    private void widgetType(int type) {
        this.setHolderValue(AccessoriesHolder::widgetType, type, "widgetType");
    }

    private int columnAmount() {
        return this.getHolderValue(AccessoriesHolder::columnAmount, 1, "columnAmount");
    }

    private void columnAmount(int type) {
        this.setHolderValue(AccessoriesHolder::columnAmount, type, "columnAmount");
    }

    public boolean leftPositioned() {
        return this.getHolderValue(AccessoriesHolder::leftPositionedAccessories, false, "leftPositioned");
    }

    private void leftPositioned(boolean value) {
        this.setHolderValue(AccessoriesHolder::leftPositionedAccessories, value, "leftPositioned");
    }

    private boolean showUnusedSlots() {
        return this.getHolderValue(AccessoriesHolder::showUnusedSlots, false, "showUnusedSlots");
    }

    private void showUnusedSlots(boolean value) {
        this.setHolderValue(AccessoriesHolder::showUnusedSlots, value, "showUnusedSlots");
    }

    private boolean showAdvancedOptions() {
        return this.getHolderValue(AccessoriesHolder::advancedOptions, false, "showAdvancedOptions");
    }

    private void showAdvancedOptions(boolean value) {
        this.setHolderValue(AccessoriesHolder::advancedOptions, value, "showAdvancedOptions");
    }

    //--

    public static void safeBatching(OwoUIDrawContext context, Consumer<MutableBoolean> drawCallback) {
        context.recordQuads();

        var hasDrawCallOccur = new MutableBoolean(false);

        drawCallback.accept(hasDrawCallOccur);

        try {
            if(hasDrawCallOccur.booleanValue()) context.submitQuads();
        } catch (Exception e) {
            var test = "this is for debugging only really!";
        }
    }
}
