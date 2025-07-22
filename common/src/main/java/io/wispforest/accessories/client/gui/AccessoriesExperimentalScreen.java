package io.wispforest.accessories.client.gui;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.screen.AccessoriesScreenTransitionHelper;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesFunkyRenderingState;
import io.wispforest.accessories.client.DrawUtils;
import io.wispforest.accessories.client.gui.components.*;
import io.wispforest.accessories.client.gui.utils.Line3d;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.impl.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import io.wispforest.accessories.menu.networking.ToggledSlots;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.networking.holder.SyncOptionChange;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import io.wispforest.accessories.pond.owo.InclusiveBoundingArea;
import io.wispforest.owo.mixin.ui.SlotAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.SpacerComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccessoriesExperimentalScreen extends BaseOwoHandledScreen<FlowLayout, AccessoriesExperimentalMenu> implements AccessoriesScreenBase<AccessoriesExperimentalMenu>, ContainerScreenExtension {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final @Nullable AbstractContainerScreen<AbstractContainerMenu> prevScreen;

    public AccessoriesExperimentalScreen(AccessoriesExperimentalMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        this.prevScreen = (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> prevScreen)
                ? (AbstractContainerScreen<AbstractContainerMenu>) prevScreen
                : null;

        this.inventoryLabelX = 42069;
    }

    //--

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    protected FlowLayout rootComponent() {
        return this.uiAdapter.rootComponent;
    }

    private boolean rebuildComponentRectangles = true;

    private List<PositionedRectangle> componentRectangles = List.of();

    public List<PositionedRectangle> getComponentRectangles() {
        if (rebuildComponentRectangles) {
            var unpackRules = new ArrayDeque<List<Pair<Boolean, String>>>();

            unpackRules.push(List.of());
            unpackRules.push(List.of(Pair.of(false, "armor_entity_layout"), Pair.of(true, "bottom_inventory_section")));
            unpackRules.push(List.of(Pair.of(false, "outer_accessories_layout")));

            var stream = rootComponent().children().stream();

            while (!unpackRules.isEmpty()) {
                var ids = unpackRules.pollLast();

                stream = stream.flatMap(component -> {
                    if (component instanceof ParentComponent parent) {
                        if (ids.isEmpty()) return parent.children().stream();

                        if (parent.id() != null) {
                            for (var pair : ids) {
                                if (pair.right().equals(parent.id())) {
                                    var componentStream = parent.children().stream();

                                    if (pair.left()) {
                                        componentStream = Stream.concat(componentStream, Stream.of(parent));
                                    }

                                    return componentStream;
                                }
                            }
                        }
                    }

                    return Stream.of(component);
                });
            }

            this.componentRectangles = stream
                    .map(component -> (PositionedRectangle) component)
                    .toList();

            this.rebuildComponentRectangles = false;
        }

        return componentRectangles;
    }

    @Override
    public <C extends io.wispforest.owo.ui.core.Component> C component(Class<C> expectedClass, String id) {
        return super.component(expectedClass, id);
    }

    //--

    private final Map<Integer, Boolean> changedSlots = new HashMap<>();

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.changedSlots.isEmpty()) return;

        var slots = this.getMenu().slots;

        var changes = this.changedSlots.entrySet().stream()
                .filter(entry -> entry.getKey() < slots.size())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.getMenu().sendMessage(new ToggledSlots(changes));

        this.changedSlots.clear();
    }

    public void hideSlot(int index) {
        hideSlot(this.menu.slots.get(index));
    }

    public void hideSlot(Slot slot) {
        ((SlotAccessor) slot).owo$setX(-300);
        ((SlotAccessor) slot).owo$setY(-300);
    }

    @Override
    public void disableSlot(Slot slot) {
        super.disableSlot(slot);

        var index = slot.index;

        // If present check result is enabled, else update as a changeed slot
        if (this.changedSlots.getOrDefault(index, false)) return;

        hideSlot(index);

        this.changedSlots.put(index, true);
    }

    @Override
    public void enableSlot(Slot slot) {
        super.enableSlot(slot);

        var index = slot.index;

        // If present check result is disabled, else update as a changed slot
        if (!this.changedSlots.getOrDefault(index, true)) return;

        this.changedSlots.put(index, false);
    }

    //--

    @Override
    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        var accessories = this.topComponent;

        return (accessories != null) ? accessories.isHovering_Logical(slot, mouseX, mouseY) : null;
    }

    //--

    @Override
    public void onClose() {
        var selectedGroups = this.getMenu().selectedGroups().stream()
                .map(SlotGroup::name)
                .collect(Collectors.toSet());

        AccessoriesNetworking
                .sendToServer(SyncOptionChange.of(PlayerOptions.FILTERED_GROUPS, selectedGroups));

        super.onClose();
    }

    @Override
    protected void init() {
        if (!menu.isValidMenu()) {
            Minecraft.getInstance().setScreen(
                    new ErrorScreen(
                            Component.literal("Accessories Screen Opening Error!"),
                            Component.literal("Unable to open Accessories Screen due to desync with the Server!")
                    ));

            return;
        }

        super.init();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (this.getOption(PlayerOptions.ADVANCED_SETTINGS)) {
            toggleAdvancedOptions(this.component(ButtonComponent.class, "advanced_options_btn"));

            return false;
        }

        return super.shouldCloseOnEsc();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AccessoriesClient.OPEN_SCREEN.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.getOption(PlayerOptions.ADVANCED_SETTINGS)) {
            toggleAdvancedOptions(this.component(ButtonComponent.class, "advanced_options_btn"));

            return false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        for (var rect : getComponentRectangles()) {
            if (rect.isInBoundingBox(mouseX, mouseY)) {
                return false;
            }
        }

        return true;
    }

    //--

    @Override
    protected int getLayerZOffset(HandledScreenLayer layer) {
        if(layer == HandledScreenLayer.CURSOR_ITEM || layer == HandledScreenLayer.ITEM_TOOLTIP) {
            return 600;
        }

        return super.getLayerZOffset(layer);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if(this.hoveredSlot != null) {
            if (this.hoveredSlot instanceof AccessoriesInternalSlot accessoriesInternalSlot) {
                if (!ArmorSlotTypes.isArmorType(accessoriesInternalSlot.slotName())) {
                    AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(this.getOption(PlayerOptions.MAIN_WIDGET_POSITION));
                }
            } else if (this.hoveredSlot instanceof ArmorSlot) {
                AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(true);
            } else if (this.hoveredSlot.container instanceof TransientCraftingContainer || this.hoveredSlot instanceof ResultSlot) {
                if (!(boolean) this.getOption(PlayerOptions.SHOW_GROUP_FILTER)) {
                    AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(!(boolean) this.getOption(PlayerOptions.MAIN_WIDGET_POSITION));
                }
            }
        }

        super.renderTooltip(guiGraphics, x, y);

        AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(false);
    }

    @Override
    protected void drawComponentTooltip(GuiGraphics drawContext, int mouseX, int mouseY, float tickDelta) {
        drawContext.push().translate(0,0, 300);
        super.drawComponentTooltip(drawContext, mouseX, mouseY, tickDelta);
        drawContext.pop();
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        var tooltipData = getTooltipFromItem(this.minecraft, itemStack);

        if (Accessories.config().screenOptions.showEquippedStackSlotType()
                && this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && this.hoveredSlot.hasItem()
                && this.hoveredSlot instanceof AccessoriesBasedSlot slot && ExtraSlotTypeProperties.getProperty(slot.slotName(), true).allowTooltipInfo()) {
            var hoveredStack = this.hoveredSlot.getItem();

            if (itemStack == hoveredStack) {
                tooltipData.add(Component.empty());
                tooltipData.add(
                        Component.translatable(Accessories.translationKey("tooltip.currently_equipped_in"))
                                .withStyle(ChatFormatting.GRAY)
                                .append(
                                        Component.translatable(slot.slotType().translation())
                                                .withStyle(ChatFormatting.BLUE)
                                )
                );
            }
        }

        return tooltipData;
    }

    private final List<Vector3d> hoveredAccessoryPositons = new ArrayList<>();
    private final List<Line3d> linesToAccessoryPositions = new ArrayList<>();

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        //--

        if (getHoveredSlot() != null && getHoveredSlot() instanceof AccessoriesInternalSlot slot && slot.isActive() && !slot.getItem().isEmpty()) {
            var positions = AccessoriesFunkyRenderingState.INSTANCE.getNotVeryNicePositions();

            var positionKey = slot.accessoriesContainer.getSlotName() + slot.getContainerSlot();

            if (positions.containsKey(positionKey)) {
                hoveredAccessoryPositons.add(positions.get(positionKey));

                var vec = positions.get(positionKey);

                if (!slot.isCosmetic && vec != null && (Accessories.config().screenOptions.hoveredOptions.line())) {
                    var start = new Vector3d(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
                    var vec3 = vec.add(0, 0, 5000);

                    linesToAccessoryPositions.add(new Line3d(start, vec3));}
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (Accessories.config().screenOptions.hoveredOptions.clickbait()) {
            hoveredAccessoryPositons.forEach(pos -> {
                        DrawUtils.blitSprite(
                                guiGraphics,
                                Accessories.of("highlight/clickbait"),
                                (int) pos.x - 128, (int) pos.y - 128, 450, 256, 256);
                    });
            hoveredAccessoryPositons.clear();
        }

        if (!linesToAccessoryPositions.isEmpty() || Accessories.config().screenOptions.hoveredOptions.line()) {
            guiGraphics.drawSpecial(multiBufferSource -> {
                var buf = multiBufferSource.getBuffer(RenderType.LINES);

                var lastPose = guiGraphics.pose().last();

                for (Line3d line : linesToAccessoryPositions) {
                    var endPoint = line.p2();

                    if (endPoint.x == 0 || endPoint.y == 0) continue;

                    var normalVec = endPoint.sub(line.p1(), new Vector3d()).normalize().get(new Vector3f());

                    double segments = Math.max(10, ((int) (line.p1().distance(line.p2()) * 10)) / 100);
                    segments *= 2;

                    var movement = (System.currentTimeMillis() / (segments * 1000) % 1);
                    var delta = movement % (2 / (segments)) % segments;

                    var firstVec = line.p1().get(new Vector3f());

                    if (delta > 0.05) {
                        DrawUtils.addToVertexBuffer(buf, firstVec, lastPose, normalVec);
                        DrawUtils.addToVertexBuffer(buf, line.lerpPoint(delta - 0.05), lastPose, normalVec);
                    }

                    for (int i = 0; i < segments / 2; i++) {
                        var delta1 = ((i * 2) / segments + movement) % 1;
                        var delta2 = ((i * 2 + 1) / segments + movement) % 1;

                        var pos1 = line.lerpPoint(delta1);
                        var pos2 = (delta2 > delta1 ? line.lerpPoint(delta2) : line.p2().get(new Vector3f()));

                        DrawUtils.addToVertexBuffer(buf, pos1, lastPose, normalVec);
                        DrawUtils.addToVertexBuffer(buf, pos2, lastPose, normalVec);
                    }
                }
            });

            minecraft.renderBuffers().bufferSource().endBatch(RenderType.LINES);

            linesToAccessoryPositions.clear();
        }
    }

    //--

    public void showCosmeticState(boolean value) {
        this.setOption(PlayerOptions.SHOW_COSMETIC_SLOTS, value);

        AccessoriesNetworking.sendToServer(PlayerOptions.SHOW_COSMETIC_SLOTS.toPacket(value));
    }

    public boolean showCosmeticState() {
        return this.getOption(PlayerOptions.SHOW_COSMETIC_SLOTS);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.changedSlots.clear();

        this.getMenu().slots.forEach(this::disableSlot);

        //--

        rootComponent.allowOverflow(true)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.VANILLA_TRANSLUCENT);

        var baseChildren = new ArrayList<io.wispforest.owo.ui.core.Component>();

        var accessoriesComponent = createAccessoriesComponent();

        //--

        var menu = this.getMenu();

        SlotGroupLoader.getValidGroups(this.getMenu().targetEntityDefaulted()).keySet().stream()
                .filter(group -> this.getOption(PlayerOptions.FILTERED_GROUPS).contains(group.name()))
                .forEach(menu::addSelectedGroup);

        //--

        var playerInv = ComponentUtils.createPlayerInv(5, menu.startingAccessoriesSlot(), this::slotAsComponent, this::enableSlot);

        this.setOption(PlayerOptions.ADVANCED_SETTINGS, false);

        var offHandIndex = this.getMenu().startingAccessoriesSlot() - 1;

        this.enableSlot(offHandIndex);

        var offhandComponent = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(this.slotAsComponent(offHandIndex).margins(Insets.of(1)))
                .padding(Insets.of(7, 7, 7, 4))
                .allowOverflow(true);

        var bottomInvComponent = Containers.horizontalFlow(Sizing.content(), Sizing.content())//Sizing.fixed(195 + 39), Sizing.fixed(88) : [39, 60]
                .child(
                        Containers.verticalFlow(Sizing.content(), Sizing.content()) // Sizing.expand()
                                .child(offhandComponent)
                                .allowOverflow(true)
                                .positioning(Positioning.absolute(-(18 + 4 + 7), 51))
                                .zIndex(10)
//                                .margins(Insets.top(54 + 4))
                )
                .child(
                        Containers.verticalFlow(Sizing.fixed(162), Sizing.fixed(76))
                                .child(playerInv)
//                                        .margins(Insets.left(4))
                                .id("bottom_component_holder")
                )
                .child(
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .positioning(Positioning.absolute(162, -7))
                                .configure((FlowLayout component) -> {
                                    if (this.getOption(PlayerOptions.SHOW_CRAFTING_GRID)) {
                                        component.child(createCraftingGrid());
                                    }
                                })
                                .id("crafting_grid_layout")
                )
                .padding(Insets.of(7)/*.add(0,0,0,22)*/)
                .surface((ctx, component) -> {
//                            ComponentUtils.getPanelSurface().draw(ctx, component);
//                            ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE.draw(ctx, component);

                    var showCraftingGrid = this.getOption(PlayerOptions.SHOW_CRAFTING_GRID);
                    var width = showCraftingGrid ? 238 : 198;

                    DrawUtils.blit(
                            ctx,
                            Accessories.of("textures/gui/theme/" + ComponentUtils.checkMode("light", "dark") + "/player_inv/" + (showCraftingGrid ? "with" : "without") + "_crafting.png"),
                            component.x() - (18 + 4), component.y(), width, 90
                    );
                })
                .allowOverflow(true)
                .id("bottom_inventory_section");

        ((InclusiveBoundingArea<?>) bottomInvComponent).addInclusionZone(offhandComponent);

        baseChildren.add(bottomInvComponent);


        //--

        var primaryLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.fixed(140))
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("armor_entity_layout");

        {
            var armorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                    .configure((FlowLayout layout) -> layout.allowOverflow(true));

            var outerLeftArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(armorSlotsLayout);

            var cosmeticArmorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .configure((FlowLayout layout) -> layout.allowOverflow(true));

            var outerRightArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(cosmeticArmorSlotsLayout);

            for (int i = 0; i < menu.addedArmorSlots() / 2; i++) {
                var armor = menu.startingAccessoriesSlot() + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                armorSlotsLayout.child(this.slotAsComponent(armor).margins(Insets.of(1)));
                cosmeticArmorSlotsLayout.child(ComponentUtils.createSlotWithToggle((AccessoriesBasedSlot) this.menu.slots.get(cosmeticArmor), this::slotAsComponent).left());
            }

            //--

            var entityContainer = Containers.stack(Sizing.content(), Sizing.fixed(126 + 14))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            createEntityComponent()
                                    ).surface((ctx, component) -> {
                                        var sideBySideMode = this.getOption(PlayerOptions.SIDE_BY_SIDE_ENTITY);

                                        DrawUtils.blit(
                                                ctx,
                                                Accessories.of("textures/gui/theme/" + ComponentUtils.checkMode("light", "dark") + "/entity_view/" + (sideBySideMode ? "double" : "single") +  "/entity_background.png"),
                                                component.x(), component.y(), sideBySideMode ? 162 : 108, 126
                                        );
                                    })
                                    .id("entity_renderer_holder")
                    )
                    .child(
                            new SpacerComponent(0){
                                @Override
                                public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                                    context.push().translate(0,0,250);
                                }
                            }
                    )
                    .child(
                            Containers.verticalFlow(Sizing.fixed(0), Sizing.fixed(0))
                                    .surface((ctx, component) -> {
                                        var surfaceType = Math.min((this.getMenu().addedArmorSlots() / 2), 4) + "_slots";
                                        var sideBySideMode = this.getOption(PlayerOptions.SIDE_BY_SIDE_ENTITY);

                                        DrawUtils.blit(
                                                ctx,
                                                Accessories.of("textures/gui/theme/" + ComponentUtils.checkMode("light", "dark") + "/entity_view/" + (sideBySideMode ? "double" : "single") + "/" + surfaceType + ".png"),
                                                component.x() - 7, component.y() - 7, sideBySideMode ? 176 : 122, 140
                                        );
                                    })
                    )
                    .child(
                            outerLeftArmorLayout
                                    .configure((FlowLayout component) -> component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true))
                                    .padding(Insets.of(7))
                                    .margins(Insets.left(-7))
                                    .positioning(Positioning.relative(0, 40))
                                    .zIndex(200) // 140
                    )
                    .child(
                            outerRightArmorLayout
                                    .configure((FlowLayout component) -> component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true))
                                    .surface(ComponentUtils.SPECTRUM_SLOT_OUTLINE)
                                    .padding(Insets.of(7))
                                    .margins(Insets.right(-7))
                                    .positioning(Positioning.relative(100, 40))
                                    .zIndex(200) // 140
                    )
                    .child(
                            ComponentUtils.createIconButton(
                                    (btn) -> {
                                        showCosmeticState(!showCosmeticState());

                                        btn.tooltip(createToggleText("slot_cosmetics", false, showCosmeticState()));

                                        var component = rootComponent().childById(AccessoriesContainingLayout.class, AccessoriesContainingLayout.defaultID());

                                        if(component != null) component.onCosmeticToggle(showCosmeticState());
                                    },
                                    14,
                                    btn -> {
                                        btn.tooltip(createToggleText("slot_cosmetics", false, showCosmeticState()))
                                                .margins(Insets.of(2, 0, 3, 0));
                                    },
                                    (btn) -> {
                                        return Accessories.of("textures/gui/" + (showCosmeticState() ? "charm" : "cosmetic") + "_toggle_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                    }
                            ).positioning(Positioning.relative(0, 0))
                    )
                    .child(
                            ComponentUtils.createIconButton(
                                    btn -> {
                                        AccessoriesScreenTransitionHelper.openPrevScreen(Minecraft.getInstance().player, this.menu.targetEntityDefaulted(), prevScreen);
                                    },
                                    10,
                                    btn -> {
                                        btn.tooltip(Component.translatable(Accessories.translationKey("back.screen")))
                                                .margins(Insets.of(3, 0, 0, 3));
                                    },
                                    (btn) -> {
                                        return Accessories.of("textures/gui/accessories_back_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                    }).positioning(Positioning.relative(100, 0))
                    ).child(
                            ComponentUtils.createIconButton(
                                    btn -> {
                                        toggleAdvancedOptions(btn);
                                    },
                                    14,
                                    "advanced_options_btn",
                                    btn -> {
                                        btn.tooltip(createToggleText("advanced_options", true, this.getOption(PlayerOptions.ADVANCED_SETTINGS)))
                                                .margins(Insets.of(0, 3, 0, 3));
                                    },
                                    (ctx, btn) -> {
                                        return Accessories.of("textures/gui/settings_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                    }).positioning(Positioning.relative(100, 100))
                    );

            if(!Accessories.config().screenOptions.alwaysShowCraftingGrid()){
                entityContainer.child(
                    createCraftingToggleButton()
                );
            }

            entityContainer
                .child(
                    new SpacerComponent(0){
                        @Override
                        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                            context.pop();
                        }
                    }
                );

            primaryLayout.child(
                entityContainer
                    .allowOverflow(true)
                    .padding(Insets.of(7))
                    .id("entity_button_panel")
            );
        }

        baseChildren.add(primaryLayout);

        if(accessoriesComponent != null) {
            primaryLayout.child((this.getOption(PlayerOptions.MAIN_WIDGET_POSITION) ? 0 : 1), accessoriesComponent); //1,
        }

        var hasSideBar = false;

        if(accessoriesComponent != null || !this.getMenu().selectedGroups().isEmpty()) {
            var sideBarHolder = createSideBarOptions();

            if (sideBarHolder != null) {
                if ((boolean) this.getOption(PlayerOptions.SIDE_WIDGET_POSITION) == this.getOption(PlayerOptions.MAIN_WIDGET_POSITION)) {
                    primaryLayout.child(0, sideBarHolder);
                } else {
                    primaryLayout.child(sideBarHolder);
                }

                hasSideBar = true;
            }
        }

        setupPadding(accessoriesComponent, hasSideBar, primaryLayout);

        //--

        var baseLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .children(baseChildren.reversed())
                .allowOverflow(true);

        baseLayout.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(50, 50));

        //--

        rootComponent.child(baseLayout);
    }

    public void setupPadding() {
        if (this.topComponent == null) return;

        var hasSideBar = this.rootComponent().childById(io.wispforest.owo.ui.core.Component.class, "side_bar_holder") != null;
        var primaryLayout = rootComponent().childById(FlowLayout.class, "armor_entity_layout");

        setupPadding(this.topComponent, hasSideBar, primaryLayout);
    }

    public void setupPadding(AccessoriesContainingLayout<?> accessoriesComponent, boolean hasSideBar, ParentComponent primaryLayout) {
        if (this.getOption(PlayerOptions.ENTITY_CENTERED)) {
            // (((Accessories Component Width) + 3) | 0) + (120) + ((3 + (30)) | 0
            var padding = 0;

            var paddingOnTheRight = this.getOption(PlayerOptions.MAIN_WIDGET_POSITION);

            if (accessoriesComponent != null) {
                var operationValue = (this.getOption(PlayerOptions.SIDE_WIDGET_POSITION) ? 1 : -1);

                // 33

                int roundingOffset = 0;

                if (this.getOption(PlayerOptions.SIDE_WIDGET_POSITION)) {
                    roundingOffset = (this.getOption(PlayerOptions.MAIN_WIDGET_POSITION) ? -1 : -3);
                } else if(component(io.wispforest.owo.ui.core.Component.class, "group_filter_holder") == null && !this.getOption(PlayerOptions.MAIN_WIDGET_POSITION)) {
                    roundingOffset = -2;
                }

                padding = accessoriesComponent.getMaxPossibleWidth() + 3 + ((hasSideBar ? 35 : 0) * operationValue) + roundingOffset;

                primaryLayout.padding(
                        paddingOnTheRight
                                ? Insets.right(padding)
                                : Insets.left(padding)
                );
            }
        } else {
            primaryLayout.padding(Insets.none());
        }
    }

    @Nullable
    private AccessoriesContainingLayout<?> topComponent = null;

    public void rebuildEntityComponent() {
        var holder = this.component(FlowLayout.class, "entity_renderer_holder");

        holder.clearChildren();

        holder.child(createEntityComponent());

        rebuildComponentRectangles = true;
    }

    public io.wispforest.owo.ui.core.Component createEntityComponent() {
        var sideBySideView = this.getOption(PlayerOptions.SIDE_BY_SIDE_ENTITY);

        return InventoryEntityComponent.of(Sizing.fixed(sideBySideView ? 162 : 108), Sizing.fixed(126), this.getMenu().targetEntityDefaulted())
                .renderWrapping((ctx, component, renderCall) -> {
                    //ScissorStack.push(component.x() + 24, component.y(), component.width() - 48, component.height(), ctx);

                    AccessoriesFunkyRenderingState.INSTANCE.wrapEntityRendering(
                            component.x() + 24, component.y(), component.x() + component.width() - 24, component.y() + component.height(),
                            primaryEntityWrapCall -> {
                                primaryEntityWrapCall.accept(() -> {
                                    renderCall.getFirst().run();
                                });

                                if (renderCall.size() != 1) {
                                    renderCall.get(1).run();
                                }
                            });

                    //ScissorStack.pop();
                })
                .sideBySideMode(sideBySideView)
                .additionalOffset(sideBySideView ? 12 : 0)
                .startingRotation(this.getOption(PlayerOptions.MAIN_WIDGET_POSITION) ? -45 : 45)
                .scaleToFit(true)
                .allowMouseRotation(true)
                .lookAtCursor(Accessories.config().screenOptions.entityLooksAtMouseCursor())
                .id("entity_rendering_component");
    }

    public void rebuildAccessoriesComponent() {

        this.getMenu().getAccessoriesSlots().forEach(this::disableSlot);

        var primaryLayout = rootComponent().childById(FlowLayout.class, "armor_entity_layout");

        //--

        var accessoriesLayout = primaryLayout.childById(AccessoriesContainingLayout.class, AccessoriesContainingLayout.defaultID());

        var accessoriesParent = accessoriesLayout.parent();

        if (accessoriesParent != null) {
            ComponentUtils.recursiveSearchSlots(accessoriesParent, slotComponent -> this.hideSlot(slotComponent.slot()));

            accessoriesParent.removeChild(accessoriesLayout);
        }

        //--

        var accessoriesComponent = createAccessoriesComponent();

        var hasSideBar = false;

        if (accessoriesComponent != null) {
            if(this.getOption(PlayerOptions.MAIN_WIDGET_POSITION)) {
                primaryLayout.child(0, accessoriesComponent);
            } else {
                primaryLayout.child(accessoriesComponent);
            }

            hasSideBar = swapOrCreateSideBarComponent();
        } else {
            if(this.getMenu().selectedGroups().isEmpty()) {
                var sideBarOptionsComponent = primaryLayout.childById(io.wispforest.owo.ui.core.Component.class, "accessories_toggle_panel");

                if (sideBarOptionsComponent != null) {
                    var sideParParent = sideBarOptionsComponent.parent();

                    if (sideParParent != null) sideParParent.removeChild(sideBarOptionsComponent);
                }
            } else {
                hasSideBar = swapOrCreateSideBarComponent();
            }
        }

        setupPadding(accessoriesComponent, hasSideBar, primaryLayout);

        toggleCraftingGrid();

        rebuildComponentRectangles = true;
    }

    public boolean swapOrCreateSideBarComponent() {
        if (this.topComponent == null && this.getMenu().selectedGroups().isEmpty()) return false;

        var armorAndEntityComp = rootComponent().childById(FlowLayout.class, "armor_entity_layout");

        armorAndEntityComp.removeChild(armorAndEntityComp.childById(FlowLayout.class, "side_bar_holder"));

        var sideBarResultWidget = createSideBarOptions();

        rebuildComponentRectangles = true;

        if (sideBarResultWidget != null) {
            if ((boolean) this.getOption(PlayerOptions.SIDE_WIDGET_POSITION) == this.getOption(PlayerOptions.MAIN_WIDGET_POSITION)) {
                armorAndEntityComp.child(0, sideBarResultWidget);
            } else {
                armorAndEntityComp.child(sideBarResultWidget);
            }

            return true;
        }


        return false;
    }

    @Nullable
    private AccessoriesContainingLayout<?> createAccessoriesComponent() {
        this.topComponent = (this.getOption(PlayerOptions.WIDGET_TYPE) == 2)
                ? ScrollableAccessoriesLayout.createOrNull(this)
                : PaginatedAccessoriesLayout.createOrNull(this);

        return this.topComponent;
    }

    //--
    @Nullable
    private io.wispforest.owo.ui.core.Component createSideBarOptions() {
        var accessoriesTogglePanel = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .id("accessories_toggle_panel");

        var groupFilterComponent = createGroupFilters();

        if(groupFilterComponent != null) {
            return Containers.verticalFlow(Sizing.content(), Sizing.content())
                    .child(
                            accessoriesTogglePanel.child(groupFilterComponent)
                                    .padding(Insets.of(7))
                                    .surface((ctx, component) -> {
                                        ComponentUtils.getPanelSurface().and(ComponentUtils.getPanelWithInset(6))
                                                .draw(ctx, component);
                                    })
                                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    )
                    .horizontalAlignment(this.getOption(PlayerOptions.MAIN_WIDGET_POSITION) ? HorizontalAlignment.LEFT : HorizontalAlignment.RIGHT)
                    .id("side_bar_holder");
        }

        return null;
    }

    public void rebuildSideBarOptions() {
        if(this.getOption(PlayerOptions.SHOW_GROUP_FILTER)) {
            var panel = rootComponent().childById(FlowLayout.class, "accessories_toggle_panel");

            if(panel != null) {
                var groupFilter = this.createGroupFilters();

                if (groupFilter != null) panel.child(groupFilter);
            }
        } else {
            var component = rootComponent().childById(ParentComponent.class, "group_filter_holder");

            if (component != null) component.remove();
        }

        rebuildComponentRectangles = true;

        this.toggleCraftingGrid();
    }

    @Nullable
    private ExtendedScrollContainer prevGroupFilterScrollable = null;

    @Nullable
    private io.wispforest.owo.ui.core.Component createGroupFilters() {
        if (!this.getOption(PlayerOptions.SHOW_GROUP_FILTER)) return null;

        var groups = new ArrayList<>(SlotGroupLoader.getValidGroups(this.getMenu().targetEntityDefaulted()).keySet());

        if (groups.isEmpty()) return null;

        var usedSlots = this.getMenu().getUsedSlots();

        var groupButtons = new ArrayList<io.wispforest.owo.ui.core.Component>();

        for (SlotGroup group : groups) {
            var groupSlots = group.slots().stream()
                    .filter(slotName -> {
                        if (UniqueSlotHandling.isUniqueSlot(slotName)) return false;

                        var slotType = SlotTypeLoader.getSlotType(this.targetEntityDefaulted(), slotName);

                        if (slotType == null) return false;

                        var capability = this.targetEntityDefaulted().accessoriesCapability();

                        if (capability == null) return false;

                        var container = capability.getContainer(slotType);

                        if (container == null) return false;

                        return container.getSize() > 0;
                    })
                    .map((slotName) -> SlotTypeLoader.INSTANCE.getSlotType(false, slotName))
                    .collect(Collectors.toSet());

            if (groupSlots.isEmpty() || (usedSlots != null && groupSlots.stream().noneMatch(usedSlots::contains))) continue;

            groupButtons.add(ComponentUtils.createGroupToggle(this, group));
        }

        if (groupButtons.isEmpty()) return null;

        var baseButtonLayout = (ParentComponent) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .children(groupButtons)
                .gap(1);

        if(groupButtons.size() > 7) {
            var scrollable = new ExtendedScrollContainer<>(
                    ScrollContainer.ScrollDirection.VERTICAL,
                    Sizing.fixed(18 + 3),
                    Sizing.fixed(108/*(5 * 14) + (5) + (2 * 2) - 1*/),
                    baseButtonLayout.padding(Insets.of(1))
            ).configure((ExtendedScrollContainer<?> scrollContainer) -> {
                scrollContainer.oppositeScrollbar((boolean) this.getOption(PlayerOptions.SIDE_WIDGET_POSITION) == this.getOption(PlayerOptions.MAIN_WIDGET_POSITION))
//                    .customClippingInsets(Insets.of(1))
                        .scrollToAfterLayout(prevGroupFilterScrollable.getProgress())
                        .scrollbarThiccness(2)
                        .scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xA0000000)))
                        .fixedScrollbarLength(16)
                        .padding(Insets.of(1, 2, 2, 1))
                        .id("group_filters_scrollable");
            });

            baseButtonLayout = scrollable;
            this.prevGroupFilterScrollable = scrollable;
        } else {
            baseButtonLayout.padding(Insets.of(1, 2, 2, 2));
            this.prevGroupFilterScrollable = null;
        }

        return Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(
                        ComponentUtils.createIconButton(
                                (btn) -> {
                                    this.getMenu().selectedGroups().clear();
                                    this.rebuildAccessoriesComponent();
                                },
                                14,
                                (btn) -> {
                                    btn.tooltip(Component.translatable(Accessories.translationKey("reset.group_filter")));
                                },
                                (btn) -> {
                                    return Accessories.of("textures/gui/reset_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                })
                                .margins(Insets.of(3, 1, 0, 0))
                )
                .child(baseButtonLayout)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("group_filter_holder");
    }

    //--

    @Nullable
    private AccessoriesScreenSettingsLayout settingsLayout = null;

    private void swapBottomComponentHolder() {
        var holder = rootComponent().childById(FlowLayout.class, "bottom_component_holder");

        holder.clearChildren();

        if(this.getOption(PlayerOptions.ADVANCED_SETTINGS)) {
            for (int i = 0; i < menu.startingAccessoriesSlot() - 1; i++) this.disableSlot(i);

            settingsLayout = new AccessoriesScreenSettingsLayout(this);

            holder.child(settingsLayout)
                    .surface(ComponentUtils.getInsetPanelSurface())
                    .padding(Insets.of(1));
        } else {
            settingsLayout = null;

            holder.child(ComponentUtils.createPlayerInv(5, menu.startingAccessoriesSlot(), this::slotAsComponent, this::enableSlot))
                    .surface(Surface.BLANK)
                    .padding(Insets.of(0));
        }

        rebuildComponentRectangles = true;
    }

    //--

    private void toggleCraftingGrid() {
        rebuildComponentRectangles = true;

        this.removeCraftingGrid();

        if (!(boolean) this.getOption(PlayerOptions.SHOW_CRAFTING_GRID)) return;

        var bottom_holder = rootComponent().childById(FlowLayout.class, "crafting_grid_layout");//side_bar_holder

        bottom_holder.child(createCraftingGrid());
    }

    private void removeCraftingGrid() {
        var bottom_holder = rootComponent().childById(FlowLayout.class, "crafting_grid_layout");//side_bar_holder

        if (bottom_holder != null) {
            bottom_holder.clearChildren();
        }

        if (!(boolean) this.getOption(PlayerOptions.SHOW_CRAFTING_GRID)) {
            bottom_holder.margins(Insets.of(0));

            for (int i = 0; i < 5; i++) this.disableSlot(i);
        }
    }

    private io.wispforest.owo.ui.core.Component createCraftingGrid() {
        return ComponentUtils.createCraftingComponent(0, this::slotAsComponent, this::enableSlot, true)
                .id("crafting_component");
    }

    private io.wispforest.owo.ui.core.Component createCraftingToggleButton() {
        return ComponentUtils.createIconButton(
            btn -> {
                AccessoriesNetworking
                    .sendToServer(SyncOptionChange.of(PlayerOptions.SHOW_CRAFTING_GRID, this.menu.owner(), bl -> !bl));

                this.setOption(PlayerOptions.SHOW_CRAFTING_GRID, !this.getOption(PlayerOptions.SHOW_CRAFTING_GRID));

                btn.tooltip(createToggleText("crafting_grid", true, this.getOption(PlayerOptions.SHOW_CRAFTING_GRID)));

                this.toggleCraftingGrid();
            },
            14,
            "crafting_grid_btn",
            btn -> {
                btn.tooltip(createToggleText("crafting_grid", true, this.getOption(PlayerOptions.SHOW_CRAFTING_GRID)))
                    .margins(Insets.of(0, 3, 3, 0));
            },
            (ctx, btn) -> {
                return Accessories.of("textures/gui/crafting_toggle_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
            }).positioning(Positioning.relative(0, 100));
    }

    //--

    @Override
    public void onHolderChange(PlayerOption<?> option) {
        if (settingsLayout == null) return;

        settingsLayout.onHolderChange(option);



        if (option.equals(PlayerOptions.SHOW_CRAFTING_GRID)) {
            var buttonPanel = component(FlowLayout.class, "entity_button_panel");

            var craftingBtn = buttonPanel.childById(io.wispforest.owo.ui.core.Component.class, "crafting_grid_btn");

            if (craftingBtn != null && Accessories.config().screenOptions.alwaysShowCraftingGrid()) {
                buttonPanel.removeChild(craftingBtn);
            } else if (craftingBtn == null && Accessories.config().screenOptions.alwaysShowCraftingGrid()) {
                buttonPanel.child(buttonPanel.children().size() - 1, createCraftingToggleButton());
            }
        }
    }

    private void toggleAdvancedOptions(ButtonComponent btn) {
        boolean value = !(boolean) this.getOption(PlayerOptions.ADVANCED_SETTINGS);
        this.setOption(PlayerOptions.ADVANCED_SETTINGS, value);

        btn.tooltip(createToggleText("advanced_options", true, this.getOption(PlayerOptions.ADVANCED_SETTINGS)));

        this.swapBottomComponentHolder();
    }

    private static Component createToggleText(String type, boolean isTooltip, boolean value) {
        return Accessories.translation(type + ".toggle." + (value ? "enabled" : "disabled") + (isTooltip ? ".tooltip" : ""));
    }

    //--

    public ExtendedSlotComponent slotAsComponent(int index) {
        return new ExtendedSlotComponent(index);
    }

    public class ExtendedSlotComponent extends SlotComponent {

        protected final AccessoriesExperimentalScreen screen;
        protected int index;

        protected ExtendedSlotComponent(int index) {
            super(index);

            this.screen = AccessoriesExperimentalScreen.this;
            this.index = index;

            this.didDraw = true;

            if (slot() instanceof AccessoriesBasedSlot accessoriesBasedSlot) {
                this.tooltip(accessoriesBasedSlot.getTooltipData());
            }
        }

        public final Slot slot() {
            return this.slot;
        }

        @Override
        public void dismount(DismountReason reason) {
            super.dismount(reason);

            if (reason == DismountReason.REMOVED) {
                ((SlotAccessor) slot).owo$setX(-300);
                ((SlotAccessor) slot).owo$setY(-300);
            }
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            //super.draw(context, mouseX, mouseY, partialTicks, delta);
            this.didDraw = true;

            ((OwoSlotExtension) this.slot).owo$setDisabledOverride(false);
        }

        @Override
        public void drawTooltip(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            var slot = this.slot();

            if(slot != null) {
                if (slot instanceof AccessoriesInternalSlot accessoriesInternalSlot) {
                    if (!ArmorSlotTypes.isArmorType(accessoriesInternalSlot.slotName())) {
                        AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(screen.getOption(PlayerOptions.MAIN_WIDGET_POSITION));
                    }
                } else if (slot instanceof ArmorSlot) {
                    AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(true);
                } else if (slot.container instanceof TransientCraftingContainer || slot instanceof ResultSlot) {
                    if (!(boolean) screen.getOption(PlayerOptions.SHOW_GROUP_FILTER)) {
                        AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(!(boolean) screen.getOption(PlayerOptions.MAIN_WIDGET_POSITION));
                    }
                }
            }

            context.translate(0, 0, 600);
            super.drawTooltip(context, mouseX, mouseY, partialTicks, delta);
            context.translate(0, 0, -600);
            AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(false);
        }
    }

    //--

    public <T> T getOption(PlayerOption<T> option) {
        return option.getDataOrDefault(this.menu.owner());
    }

    public <T> void setOption(PlayerOption<T> option, T data) {
        option.setData(this.menu.owner(), data);
    }
}
