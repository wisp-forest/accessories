package top.theillusivec4.curios.compat;

import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.capability.CurioInventory;

import java.util.*;
import java.util.function.Predicate;

public record WrappedCurioItemHandler(AccessoriesCapabilityImpl capability) implements ICuriosItemHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public WrappedCurioItemHandler(AccessoriesCapabilityImpl capability) {
        this.capability = capability;

        var entity = this.capability.entity();

        if (entity.hasData(CuriosRegistry.INVENTORY)) {
            var inv = entity.getData(CuriosRegistry.INVENTORY);

            inv.init(capability);

            entity.removeData(CuriosRegistry.INVENTORY);
        }
    }

    public static void attemptConversion(AccessoriesCapabilityImpl capability) {
        if (!capability.entity().hasData(CuriosRegistry.INVENTORY)) return;

        new WrappedCurioItemHandler(capability);
    }

    @Override
    public Map<String, ICurioStacksHandler> getCurios() {
        var handlers = new HashMap<String, ICurioStacksHandler>();

        this.capability().getContainers()
                .forEach((s, container) -> handlers.put(CuriosWrappingUtils.accessoriesToCurios(s), new WrappedCurioStackHandler((AccessoriesContainerImpl) container)));

        handlers.put("curio", new EmptyCuriosStackHandler("curio"));

        return handlers;
    }

    @Override
    public void setCurios(Map<String, ICurioStacksHandler> map) {
    }

    @Override
    public int getSlots() {
        int totalSlots = 0;

        for (var stacks : this.getCurios().values()) totalSlots += stacks.getSlots();

        return totalSlots;
    }

    @Override
    public void reset() {
        this.capability.reset(false);
    }

    @Override
    public Optional<ICurioStacksHandler> getStacksHandler(String identifier) {
        return Optional.ofNullable(this.capability.getContainers().get(identifier))
                .map(container -> new WrappedCurioStackHandler((AccessoriesContainerImpl) container));
    }

    @Override
    public IItemHandlerModifiable getEquippedCurios() {
        var curios = this.getCurios();
        var itemHandlers = new IItemHandlerModifiable[curios.size()];
        int index = 0;

        for (ICurioStacksHandler stacksHandler : curios.values()) {
            if (index < itemHandlers.length) {
                itemHandlers[index] = stacksHandler.getStacks();
                index++;
            }
        }
        return new CombinedInvWrapper(itemHandlers);
    }

    @Override
    public void setEquippedCurio(String identifier, int index, ItemStack stack) {
        Optional.ofNullable(this.capability.getContainers().get(identifier))
                .ifPresent(container -> container.getAccessories().setItem(index, stack));
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Item item) {
        return findFirstCurio((stack -> stack.getItem().equals(item)));
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> filter) {
        return Optional.ofNullable(this.capability.getFirstEquipped(filter))
                .map(entry -> new SlotResult(CuriosWrappingUtils.create(entry.reference()), entry.stack()));
    }

    @Override
    public List<SlotResult> findCurios(Item item) {
        return findCurios(stack -> stack.getItem().equals(item));
    }

    @Override
    public List<SlotResult> findCurios(Predicate<ItemStack> filter) {
        return this.capability.getEquipped(filter)
                .stream()
                .map(entry -> new SlotResult(CuriosWrappingUtils.create(entry.reference()), entry.stack()))
                .toList();
    }

    @Override
    public List<SlotResult> findCurios(String... identifiers) {
        var containerIds = Set.of(identifiers);

        return this.capability.getContainers().entrySet().stream()
                .filter(entry -> containerIds.contains(entry.getKey()))
                .map(entry -> {
                    var container = entry.getValue();
                    var accessories = entry.getValue().getAccessories();

                    var references = new ArrayList<SlotEntryReference>();

                    for (var stackEntry : accessories) {
                        var stack = stackEntry.getSecond();
                        var reference = container.createReference(stackEntry.getFirst());

                        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

                        references.add(new SlotEntryReference(reference, stack));

                        if (accessory instanceof AccessoryNest holdable) {
                            for (ItemStack innerStack : holdable.getInnerStacks(stackEntry.getSecond())) {
                                references.add(new SlotEntryReference(reference, innerStack));
                            }
                        }
                    }

                    return references;
                }).flatMap(references -> {
                    return references.stream().map(entry -> new SlotResult(CuriosWrappingUtils.create(entry.reference()), entry.stack()));
                }).toList();
    }

    @Override
    public Optional<SlotResult> findCurio(String identifier, int index) {
        return Optional.ofNullable(this.capability.getContainers().get(identifier))
                .flatMap(container -> {
                    var stack = container.getAccessories().getItem(index);

                    if (stack.isEmpty()) return Optional.empty();

                    return Optional.of(new SlotResult(new SlotContext(identifier, this.capability.entity(), 0, false, true), stack));
                });
    }

    @Override
    public LivingEntity getWearer() {
        return this.capability.entity();
    }

    @Override
    public void loseInvalidStack(ItemStack stack) {
    }

    @Override
    public void handleInvalidStacks() {
    }

    @Override
    public int getFortuneLevel(@Nullable LootContext lootContext) {
        return 0;
    }

    @Override
    public int getLootingLevel(DamageSource source, LivingEntity target, int baseLooting) {
        return 0;
    }

    @Override
    public Set<ICurioStacksHandler> getUpdatingInventories() {
        return null;
    }

    @Override
    public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        this.capability.addTransientSlotModifiers(modifiers);
    }

    @Override
    public void addPermanentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        this.capability.addPersistentSlotModifiers(modifiers);
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        this.capability.removeSlotModifiers(modifiers);
    }

    @Override
    public void clearSlotModifiers() {
        this.capability.clearSlotModifiers();
    }

    @Override
    public Multimap<String, AttributeModifier> getModifiers() {
        return this.capability.getSlotModifiers();
    }

    @Override
    public ListTag saveInventory(boolean clear) {
        var compound = new CompoundTag();

        ((AccessoriesHolderImpl)this.capability().getHolder())
                .write(compound);

        var outerCompound = new CompoundTag();

        outerCompound.put("main_data", compound);
        outerCompound.putBoolean("is_accessories_data", true);

        var list = new ListTag();

        list.add(outerCompound);

        return list;
    }

    @Override
    public void loadInventory(ListTag data) {
        var compound = data.getCompound(0);

        try {
            if(compound.contains("is_accessories_data")){
                ((AccessoriesHolderImpl)this.capability().getHolder())
                        .read(compound.getCompound("main_data"));
            } else {
                CurioInventory.readData(this.getWearer(), this.capability(), data);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load a wrapped curio inventory as a error occurred, such will not be loaded!", e);
        }
    }

    @Override
    public Tag writeTag() {
        return new CompoundTag();
    }

    @Override
    public void readTag(Tag tag) {
    }

    @Override
    public void clearCachedSlotModifiers() {
        this.capability.clearCachedSlotModifiers();
    }

    @Override
    public void growSlotType(String identifier, int amount) {
    }

    @Override
    public void shrinkSlotType(String identifier, int amount) {
    }
}
