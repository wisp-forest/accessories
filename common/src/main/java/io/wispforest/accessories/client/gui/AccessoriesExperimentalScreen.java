package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.components.*;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.mixin.client.AbstractContainerScreenAccessor;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.wispforest.accessories.client.gui.components.ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE;

public class AccessoriesExperimentalScreen extends BaseOwoHandledScreen<FlowLayout, AccessoriesExperimentalMenu> implements AccessoriesScreenBase, ContainerScreenExtension {

    public AccessoriesExperimentalScreen(AccessoriesExperimentalMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        this.inventoryLabelX = 42069;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    public ExtendedSlotComponent slotAsComponent(int index) {
        return new ExtendedSlotComponent(this, index);
    }

    //--

    public final Set<Integer> changedSlots = new HashSet<>();

    @Override
    public void disableSlot(int index) {
        super.disableSlot(index);

        changedSlots.add(index);
    }

    @Override
    public void disableSlot(Slot slot) {
        super.disableSlot(slot);

        changedSlots.add(slot.index);
    }

    @Override
    public void enableSlot(int index) {
        super.enableSlot(index);

        changedSlots.add(index);
    }

    @Override
    public void enableSlot(Slot slot) {
        ((OwoSlotExtension) slot).owo$setDisabledOverride(false);

        changedSlots.add(slot.index);
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

        if(!changedSlots.isEmpty()) {
            var slots = this.getMenu().slots;

            var changes = changedSlots.stream()
                    .map(i -> (i < slots.size()) ? slots.get(i) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(slot -> slot.index, slot -> ((OwoSlotExtension)slot).owo$getDisabledOverride()));

            this.getMenu().sendMessage(new AccessoriesExperimentalMenu.ToggledSlots(changes));

            changedSlots.clear();
        }
    }

    @Override
    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
    }

    //--

    public boolean showCosmeticState = false;

    //--

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

        baseChildren.add(ComponentUtils.createPlayerInv(menu.startingAccessoriesSlot, this::slotAsComponent, this::enableSlot));

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

            for (int i = 0; i < menu.addedArmorSlots / 2; i++) {
                var armor = menu.startingAccessoriesSlot + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                var armorSlot = this.slotAsComponent(armor)
                        .margins(Insets.of(1));

                var cosmeticArmorSlot = this.slotAsComponent(cosmeticArmor)
                        .margins(Insets.of(1));

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

            var entityContainer = Containers.stack(Sizing.content(), Sizing.fixed(131 + 12))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            InventoryEntityComponent.of(Sizing.fixed(131), Sizing.fixed(108), this.targetEntityDefaulted())
                                                    .scaleToFit(true)
                                                    .allowMouseRotation(true)
                                    )
                                    .surface(Surface.flat(Color.BLACK.argb()))
                    )
                    .child(
                            outerLeftArmorLayout.positioning(Positioning.relative(0, 45))
                                    .margins(Insets.left(-6))
                                    .zIndex(10)
                    )
                    .child(
                            outerRightArmorLayout.positioning(Positioning.relative(100, 45))
                                    .margins(Insets.right(-6))
                                    .zIndex(10)
                    )
                    .padding(Insets.of(6))
                    .surface(Surface.PANEL);

            var offHandIndex = this.getMenu().startingAccessoriesSlot - (this.getMenu().includeSaddle ? 2 : 1);

            this.enableSlot(offHandIndex);

            if(this.getMenu().includeSaddle) {
                var saddleIndex = this.getMenu().startingAccessoriesSlot - 1;

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

            armorAndEntityLayout.child(0, entityContainer);
        }

        baseChildren.add(armorAndEntityLayout);

        //--

        var cosmeticToggleButton = Components.button(Component.literal(""), btn -> {
            var showCosmeticState = !this.showCosmeticState;

            this.showCosmeticState = showCosmeticState;

            var component = this.uiAdapter.rootComponent.childById(AccessoriesContainingComponent.class, AccessoriesContainingComponent.defaultID());

            if(component != null) component.onCosmeticToggle(this.showCosmeticState);
        }).renderer((context, button, delta) -> {
            ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

            var textureAtlasSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(!showCosmeticState ? Accessories.of("gui/slot/cosmetic") : Accessories.of("gui/slot/charm"));

            var color = (!showCosmeticState ? Color.WHITE.interpolate(Color.BLACK, 0.3f) : Color.BLACK);

            var red = color.red();
            var green = color.green();
            var blue = color.blue();

            var shard = RenderStateShard.TRANSLUCENT_TRANSPARENCY;

            //shard.setupRenderState();

            if(!showCosmeticState) {
                GuiGraphicsUtils.drawWithSpectrum(context, button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, 1f);
                context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.4f);
            } else {
                context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.9f);
            }

            //shard.clearRenderState();
        }).sizing(Sizing.fixed(20));

        var accessoriesComponent = createAccessoriesComponent();

        if(accessoriesComponent != null) armorAndEntityLayout.child(0, accessoriesComponent);

        //--

        var optionPanel = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .child(
                        Components.button(
                                        unusedSlotsToggleButton(this.getMenu().areUnusedSlotsShown()),
                                        btn -> {
                                            AccessoriesInternals.getNetworkHandler()
                                                    .sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, this.getMenu().owner(), bl -> !bl));
                                        }
                                ).horizontalSizing(Sizing.fixed(100))
                                .id("unused_slots_toggle")
                )
                .gap(3)
                .surface(Surface.PANEL)
                .padding(Insets.of(5));

        if(!this.menu.addedSlots.isEmpty()) optionPanel.child(cosmeticToggleButton);

        {
            var min = 3;
            var max = 12;

            var step = 1f / (max - min);

            var rowAmountSlider = Components.discreteSlider(Sizing.fixed(60), min, max)
                    .snap(true)
                    .setFromDiscreteValue(this.menu.owner().accessoriesHolder().columnAmount())
                    .scrollStep(step);

            rowAmountSlider.onChanged().subscribe(value -> {
                this.menu.owner().accessoriesHolder().columnAmount((int) value);

                var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

                for (var child : armorAndEntityComp.children()) {
                    if (AccessoriesContainingComponent.defaultID().equals(child.id()) && child instanceof AccessoriesContainingComponent accessories) {
                        var parent = accessories.parent();

                        if (parent != null) {
                            parent.queue(() -> {
                                var currentParent = accessories.parent();

                                if (currentParent != null) currentParent.removeChild(accessories);
                            });
                        }
                    }
                }

                var accessoriesComp = createAccessoriesComponent();

                if (accessoriesComp != null) {
                    armorAndEntityComp.child(0, accessoriesComp);
                }
            });

            optionPanel.child(rowAmountSlider);
        }

        {
            var min = 1;
            var max = 2;

            var step = 1f / (max - min);

            var widgetTypeSlider = Components.discreteSlider(Sizing.fixed(60), min, max)
                    .snap(true)
                    .setFromDiscreteValue(this.menu.owner().accessoriesHolder().widgetType())
                    .scrollStep(step);

            widgetTypeSlider.onChanged().subscribe(value -> {
                this.menu.owner().accessoriesHolder().widgetType((int) value);

                var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

                for (var child : armorAndEntityComp.children()) {
                    if (AccessoriesContainingComponent.defaultID().equals(child.id()) && child instanceof AccessoriesContainingComponent accessories) {
                        var parent = accessories.parent();

                        if (parent != null) {
                            parent.queue(() -> {
                                var currentParent = accessories.parent();

                                if (currentParent != null) currentParent.removeChild(accessories);
                            });
                        }
                    }
                }

                var accessoriesComp = createAccessoriesComponent();

                if (accessoriesComp != null) {
                    armorAndEntityComp.child(0, accessoriesComp);
                }
            });

            optionPanel.child(widgetTypeSlider);
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
    private AccessoriesContainingComponent createAccessoriesComponent() {
        var widgetType = this.menu.owner().accessoriesHolder().widgetType();

        return switch (widgetType) {
            case 3 -> null;
            case 2 -> null;
            default -> GriddedAccessoriesComponent.createOrNull(this);
        };
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        var accessories = this.uiAdapter.rootComponent.childById(AccessoriesContainingComponent.class, AccessoriesContainingComponent.defaultID());

        if(accessories != null) {
            var result = accessories.isHovering_Logical(slot, mouseX, mouseY);

            if(result != null) return result;
        }

        return ContainerScreenExtension.super.isHovering_Logical(slot, mouseX, mouseY);
    }

    @Override
    @Nullable
    public Boolean isHovering_Rendering(Slot slot, double mouseX, double mouseY) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : ContainerScreenExtension.super.isHovering_Rendering(slot, mouseX, mouseY);
    }

    @Override
    public @Nullable Boolean shouldRenderSlot(Slot slot) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : ContainerScreenExtension.super.shouldRenderSlot(slot);
    }

    @Override
    public void onHolderChange(String key) {
        switch (key) {
            case "unused_slots" -> updateUnusedSlotToggleButton();
        }
    }

    public void updateUnusedSlotToggleButton() {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "unused_slots_toggle");
        btn.setMessage(unusedSlotsToggleButton(this.menu.areUnusedSlotsShown()));
        this.menu.reopenMenu();
    }

    private static Component unusedSlotsToggleButton(boolean value) {
        return createToggleTooltip("unused_slots", value);
    }

    private static Component cosmeticsToggleTooltip(boolean value) {
        return createToggleTooltip("slot.cosmetics", value);
    }

    private static Component createToggleTooltip(String type, boolean value) {
        var key = type + ".toggle." + (!value ? "show" : "hide");

        return Component.translatable(Accessories.translation(key));
    }

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/slot.png");

    public class ExtendedSlotComponent extends SlotComponent {

        private boolean isBatched = false;

        protected final AccessoriesExperimentalScreen screen;
        protected final int index;

        protected ExtendedSlotComponent(AccessoriesExperimentalScreen screen, int index) {
            super(index);

            this.screen = screen;
            this.index = index;
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
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if(!this.screen.isSlotEnabled(index())) {
                this.didDraw = true;
            } else if(slot() instanceof SlotTypeAccessible) {
                this.didDraw = true;

                if(isBatched) return;

                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();

                //RenderStateShard.LEQUAL_DEPTH_TEST.setupRenderState();

                renderSlot(context);

                renderCosmeticOverlay(context, false);

                renderHover(context, () -> screen.hoveredSlot);

//                RenderSystem.disableBlend();
//                RenderSystem.disableDepthTest();
            } else {
                super.draw(context, mouseX, mouseY, partialTicks, delta);
            }
        }

        public void renderSlot(OwoUIDrawContext context) {
            int i = screen.leftPos;
            int j = screen.topPos;

            boolean bl = true;

            if (bl) {
                context.push();
                context.translate((float) i, (float) j, 0);

                screen.forceRenderSlot(context, slot());

                context.pop();
            }
        }

        public void renderCosmeticOverlay(OwoUIDrawContext context, boolean externalBatching) {
            if(!(slot() instanceof SlotTypeAccessible slotTypeAccessible)) return;

            if (slotTypeAccessible.isCosmeticSlot()) {
                //GuiGraphicsUtils.blitWithAlpha(context, Accessories.of("textures/gui/slot_frame.png"), this.x(), this.y(), 0, 0, 16, 16, 18, 18, new Vector4f(0.2f, 0.8f, 0.8f, 1.0f));

                //context.blit(Accessories.of("textures/gui/slot_frame.png"), this.x() - 1, this.y() - 1, 0, 0, 18, 18, 18,18);
                //RenderStateShard.TRANSLUCENT_TRANSPARENCY.setupRenderState();
                context.push();
                context.translate(0.0F, 0.0F, 101.0F);

                if(externalBatching) {
                    GuiGraphicsUtils.drawRectOutlineWithSpectrumWithoutRecord(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);
                } else {
                    GuiGraphicsUtils.drawRectOutlineWithSpectrum(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);

                }
                context.pop();
            }
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
        context.push();

        if(false) {
            safeBatching(context, hasDrawCallOccur -> {
                ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
                    if(!this.isSlotEnabled(slotComponent.index())) return;

                    if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

                    slotComponent.renderSlot(context);

                    hasDrawCallOccur.setValue(true);
                });
            });
        } else {
            renderSlotsBatched(context, component);
        }

        safeBatching(context, hasDrawCallOccur -> {
            ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
                if(!this.isSlotEnabled(slotComponent.index())) return;

                if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

                slotComponent.renderCosmeticOverlay(context, true);

                hasDrawCallOccur.setValue(true);
            });
        });

        ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
            if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

            slotComponent.renderHover(context, () -> this.hoveredSlot);
        });

        context.pop();
    };

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

    public void renderSlotsBatched(OwoUIDrawContext context, ParentComponent parentComponent) {

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
            ComponentUtils.recursiveSearch(parentComponent, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
                if(!this.isSlotEnabled(slotComponent.index())) return;

                var slot = slotComponent.slot();

                if(!slotComponent.isBatched() || !(slot instanceof SlotTypeAccessible)) return;

                var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                if(data == null) return;

                var itemStack = data.getA();

                if (itemStack.isEmpty() && slot.isActive()) {
                    var pair = slot.getNoItemIcon();

                    if (pair != null) {
                        var textureAtlasSprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());

                        context.blit(slot.x, slot.y, 0, 16, 16, textureAtlasSprite);

                        allBl2s.put(slot, true);
                    }
                }

                hasDrawCallOccur.setValue(true);
            });
        });

        //--

        //if(true) return;

        safeBatching(context, hasDrawCallOccur -> {
            ComponentUtils.recursiveSearch(parentComponent, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
                if(!this.isSlotEnabled(slotComponent.index())) return;

                var slot = slotComponent.slot();

                if(!slotComponent.isBatched() || !(slot instanceof SlotTypeAccessible)) return;

                var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                if(data == null) return;

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
            });
        });

        context.pop();

        //--

        context.pop();
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

    public final Surface FULL_SLOT_RENDERING = BACKGROUND_SLOT_RENDERING_SURFACE.and(SLOT_RENDERING_SURFACE);

}
