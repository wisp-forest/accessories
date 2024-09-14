package io.wispforest.accessories.impl;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.endec.format.nbt.NbtEndec;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@ApiStatus.Internal
public class AccessoriesHolderImpl implements AccessoriesHolder, InstanceEndec {

    private static final MapCarrier EMPTY = new NbtMapCarrier(new CompoundTag());

    private final Map<String, AccessoriesContainer> slotContainers = new LinkedHashMap<>();

    public final List<ItemStack> invalidStacks = new ArrayList<>();
    protected final Map<AccessoriesContainer, Boolean> containersRequiringUpdates = new HashMap<>();

    //-- Logical Stuff

    private PlayerEquipControl equipControl = PlayerEquipControl.MUST_CROUCH;

    //--

    //-- Rendering Stuff

    private boolean showAdvancedOptions = false;

    private boolean showUnusedSlots = false;
    private boolean showUniqueSlots = false;

    private boolean cosmeticsShown = false;
    private boolean linesShown = false;

    private int columnAmount = 1;
    private int widgetType = 2;
    private boolean showGroupFilter = false;
    private boolean mainWidgetPosition = true;
    private boolean sideWidgetPosition = false;

    private boolean showCraftingGrid = false;

    // --

    private MapCarrier carrier;
    protected boolean loadedFromTag = false;

    public static AccessoriesHolderImpl of(){
        var holder = new AccessoriesHolderImpl();

        holder.loadedFromTag = true;
        holder.carrier = EMPTY;

        return holder;
    }

    @ApiStatus.Internal
    protected Map<String, AccessoriesContainer> getSlotContainers() {
        return this.slotContainers;
    }

    //--

    @Override
    public PlayerEquipControl equipControl() {
        return equipControl;
    }

    @Override
    public AccessoriesHolder equipControl(PlayerEquipControl value) {
        this.equipControl = value;

        return this;
    }

    //--

    @Override
    public boolean showUnusedSlots() {
        return this.showUnusedSlots;
    }

    @Override
    public AccessoriesHolder showUnusedSlots(boolean value) {
        this.showUnusedSlots = value;

        return this;
    }

    @Override
    public boolean showUniqueSlots() {
        return this.showUniqueSlots;
    }

    @Override
    public AccessoriesHolder showUniqueSlots(boolean value) {
        this.showUniqueSlots = value;

        return this;
    }

    @Override
    public boolean cosmeticsShown() {
        return this.cosmeticsShown;
    }

    @Override
    public AccessoriesHolder cosmeticsShown(boolean value) {
        this.cosmeticsShown = value;

        return this;
    }

    @Override
    public boolean linesShown() {
        return this.linesShown;
    }

    @Override
    public AccessoriesHolder linesShown(boolean value) {
        this.linesShown = value;

        return this;
    }

    @Override
    public int columnAmount() {
        return Math.max(columnAmount, 1);
    }

    @Override
    public AccessoriesHolder columnAmount(int value) {
        this.columnAmount = value;

        return this;
    }

    @Override
    public int widgetType() {
        return Math.max(widgetType, 1);
    }

    @Override
    public AccessoriesHolder widgetType(int value) {
        this.widgetType = value;

        return this;
    }

    @Override
    public boolean mainWidgetPosition() {
        return this.mainWidgetPosition;
    }

    @Override
    public AccessoriesHolder mainWidgetPosition(boolean value) {
        this.mainWidgetPosition = value;

        return this;
    }

    @Override
    public boolean showAdvancedOptions() {
        return this.showAdvancedOptions;
    }

    @Override
    public AccessoriesHolder showAdvancedOptions(boolean value) {
        this.showAdvancedOptions = value;

        return this;
    }

    public boolean showGroupFilter() {
        return this.showGroupFilter;
    }

    public AccessoriesHolder showGroupFilter(boolean value) {
        this.showGroupFilter = value;

        return this;
    }

    public boolean sideWidgetPosition() {
        return this.sideWidgetPosition;
    }

    public AccessoriesHolder sideWidgetPosition(boolean value) {
        this.sideWidgetPosition = value;

        return this;
    }

    @Override
    public boolean showCraftingGrid() {
        return this.showCraftingGrid;
    }

    @Override
    public AccessoriesHolder showCraftingGrid(boolean value) {
        this.showCraftingGrid = value;

        return this;
    }

    //--


    public void init(AccessoriesCapability capability) {
        var livingEntity = capability.entity();

        this.slotContainers.clear();
        //this.invalidStacks.clear();

        if (loadedFromTag) {
            EntitySlotLoader.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.putIfAbsent(s, new AccessoriesContainerImpl(capability, slotType));
            });

            var ctx = SerializationContext.attributes(
                    new EntityAttribute(livingEntity),
                    RegistriesAttribute.of(livingEntity.registryAccess())
            );

            read(capability, livingEntity, this.carrier, ctx);
        } else {
            EntitySlotLoader.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.put(s, new AccessoriesContainerImpl(capability, slotType));
            });
        }
    }

    // TODO: SPLIT DECODING AND VALIDATION SAFETY DOWN THE ROAD
    private static final KeyedEndec<Map<String, AccessoriesContainer>> CONTAINERS_KEY = NbtEndec.COMPOUND.xmapWithContext(
            (ctx, containersMap) -> {
                var entity = ctx.requireAttributeValue(EntityAttribute.ENTITY).livingEntity();
                var slotContainers = ctx.requireAttributeValue(ContainersAttribute.CONTAINERS).slotContainers();
                var invalidStacks = ctx.requireAttributeValue(InvalidStacksAttribute.INVALID_STACKS).invalidStacks();

                var slots = EntitySlotLoader.getEntitySlots(entity);

                for (var key : containersMap.getAllKeys()) {
                    var containerElement = containersMap.getCompound(key);

                    if (containerElement.isEmpty()) continue; // TODO: Handle this case?

                    if (slots.containsKey(key)) {
                        var container = slotContainers.get(key);
                        var prevAccessories = AccessoriesContainerImpl.copyContainerList(container.getAccessories());
                        var prevCosmetics = AccessoriesContainerImpl.copyContainerList(container.getCosmeticAccessories());

                        ((AccessoriesContainerImpl) container).read(new NbtMapCarrier(containerElement), ctx);

                        if (prevAccessories.getContainerSize() > container.getSize()) {
                            for (int i = container.getSize() - 1; i < prevAccessories.getContainerSize(); i++) {
                                var prevStack = prevAccessories.getItem(i);

                                if (!prevStack.isEmpty()) invalidStacks.add(prevStack);

                                var prevCosmetic = prevCosmetics.getItem(i);

                                if (!prevCosmetic.isEmpty()) invalidStacks.add(prevCosmetic);
                            }
                        }
                    } else {
                        var containers = AccessoriesContainerImpl.readContainers(new NbtMapCarrier(containerElement), ctx, AccessoriesContainerImpl.COSMETICS_KEY, AccessoriesContainerImpl.ITEMS_KEY);

                        for (var simpleContainer : containers) {
                            for (int i = 0; i < simpleContainer.getContainerSize(); i++) {
                                var stack = simpleContainer.getItem(i);

                                if (!stack.isEmpty()) invalidStacks.add(stack);
                            }
                        }
                    }
                }

                return slotContainers;
            }, (ctx, containers) -> {
                var containerMap = new CompoundTag();

                containers.forEach((s, container) -> {
                    containerMap.put(s, Util.make(NbtMapCarrier.of(), innerCarrier -> ((AccessoriesContainerImpl) container).write(innerCarrier, ctx)).compoundTag());
                });

                return containerMap;
            }).keyed("accessories_containers", HashMap::new);

    private static final KeyedEndec<Boolean> COSMETICS_SHOWN_KEY = Endec.BOOLEAN.keyed("cosmetics_shown", false);
    private static final KeyedEndec<Boolean> LINES_SHOWN_KEY = Endec.BOOLEAN.keyed("lines_shown", false);
    private static final KeyedEndec<PlayerEquipControl> EQUIP_CONTROL_KEY = Endec.forEnum(PlayerEquipControl.class).keyed("equip_control", PlayerEquipControl.MUST_CROUCH);

    @Override
    public void write(MapCarrier carrier, SerializationContext ctx) {
        if(slotContainers.isEmpty()) return;

        carrier.put(COSMETICS_SHOWN_KEY, this.cosmeticsShown);
        carrier.put(LINES_SHOWN_KEY, this.linesShown);
        carrier.put(EQUIP_CONTROL_KEY, this.equipControl);

        carrier.put(ctx, CONTAINERS_KEY, this.slotContainers);
    }

    public void read(LivingEntity entity, MapCarrier carrier, SerializationContext ctx) {
        read(entity.accessoriesCapability(), entity, carrier, ctx);
    }

    public void read(AccessoriesCapability capability, LivingEntity entity, MapCarrier carrier, SerializationContext ctx) {
        this.loadedFromTag = false;

        EndecUtils.dfuKeysCarrier(
                carrier,
                Map.of(
                        "AccessoriesContainers", "accessories_containers",
                        "CosmeticsShown", "cosmetics_shown",
                        "LinesShown", "lines_shown",
                        "EquipControl", "equip_control"
                ));

        this.cosmeticsShown = carrier.get(COSMETICS_SHOWN_KEY);
        this.linesShown = carrier.get(LINES_SHOWN_KEY);
        this.equipControl = carrier.get(EQUIP_CONTROL_KEY);

        carrier.getWithErrors(ctx.withAttributes(new ContainersAttribute(this.slotContainers), new InvalidStacksAttribute(this.invalidStacks)), CONTAINERS_KEY);

        capability.clearCachedSlotModifiers();

        this.carrier = EMPTY;
    }

    @Override
    public void read(MapCarrier carrier, SerializationContext context) {
        this.loadedFromTag = true;

        this.carrier = carrier;
    }

    private record ContainersAttribute(Map<String, AccessoriesContainer> slotContainers) implements SerializationAttribute.Instance {
        public static final SerializationAttribute.WithValue<ContainersAttribute> CONTAINERS = SerializationAttribute.withValue(Accessories.translationKey("containers"));

        @Override public SerializationAttribute attribute() { return CONTAINERS; }
        @Override public Object value() { return this; }
    }

    private record InvalidStacksAttribute(List<ItemStack> invalidStacks) implements SerializationAttribute.Instance {
        public static final SerializationAttribute.WithValue<InvalidStacksAttribute> INVALID_STACKS = SerializationAttribute.withValue(Accessories.translationKey("invalidStacks"));

        @Override public SerializationAttribute attribute() { return INVALID_STACKS; }
        @Override public Object value() { return this; }
    }

    private record EntityAttribute(LivingEntity livingEntity) implements SerializationAttribute.Instance{
        public static final SerializationAttribute.WithValue<EntityAttribute> ENTITY = SerializationAttribute.withValue("entity");

        @Override public SerializationAttribute attribute() { return ENTITY; }
        @Override public Object value() { return this;}
    }
}