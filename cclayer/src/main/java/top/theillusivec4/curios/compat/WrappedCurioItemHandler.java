package top.theillusivec4.curios.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.endec.SerializationContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
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
import java.util.function.Supplier;

public class WrappedCurioItemHandler implements ICuriosItemHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Supplier<AccessoriesCapabilityImpl> capabilitySup;

    public WrappedCurioItemHandler(Supplier<AccessoriesCapabilityImpl> capabilitySup) {
        this.capabilitySup = capabilitySup;
    }

    public Optional<AccessoriesCapabilityImpl> capability() {
        var capability = this.capabilitySup.get();

        var cap = capability.entity().getCapability(CuriosCapability.INVENTORY);

        return Optional.ofNullable(capability);
    }

    public static void attemptConversion(Supplier<AccessoriesCapabilityImpl> capability) {
        var cap = capability.get().entity().getCapability(CuriosCapability.INVENTORY);

        if (!cap.isPresent()) return;

        new WrappedCurioItemHandler(capability);
    }

    @Override
    public Map<String, ICurioStacksHandler> getCurios() {
        var handlers = new HashMap<String, ICurioStacksHandler>();

        var capability = this.capability();

        capability.ifPresentOrElse(cap -> {
            cap.getContainers()
                    .forEach((s, container) -> handlers.put(CuriosWrappingUtils.accessoriesToCurios(s), new WrappedCurioStackHandler((AccessoriesContainerImpl) container)));
        }, () -> {
            LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!");
        });

        if (capability.isEmpty()) return Map.of();

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
        this.capability().ifPresent(capability -> capability.reset(false));
    }

    @Override
    public Optional<ICurioStacksHandler> getStacksHandler(String identifier) {
        return this.capability()
                .flatMap(capability -> {
                    return Optional.ofNullable((AccessoriesContainerImpl) capability.getContainers().get(identifier))
                            .map(WrappedCurioStackHandler::new);
                });
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
        this.capability().ifPresent(capability -> {
            var container = capability.getContainers().get(identifier);

            if (container != null) container.getAccessories().setItem(index, stack);
        });
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Item item) {
        return findFirstCurio((stack -> stack.getItem().equals(item)));
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> filter) {
        return this.capability()
                .flatMap(capability -> Optional.ofNullable(capability.getFirstEquipped(filter)))
                .map(entry -> new SlotResult(CuriosWrappingUtils.create(entry.reference()), entry.stack()));
    }

    @Override
    public List<SlotResult> findCurios(Item item) {
        return findCurios(stack -> stack.getItem().equals(item));
    }

    @Override
    public List<SlotResult> findCurios(Predicate<ItemStack> filter) {
        return this.capability()
                .stream()
                .flatMap(capability -> capability.getEquipped(filter).stream())
                .map(entry -> new SlotResult(CuriosWrappingUtils.create(entry.reference()), entry.stack()))
                .toList();
    }

    @Override
    public List<SlotResult> findCurios(String... identifiers) {
        var containerIds = Set.of(identifiers);

        var capability = this.capability();

        if (capability.isEmpty()) return List.of();

        return capability.get().getContainers().entrySet().stream()
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
        var capability = this.capability();

        return capability
                .flatMap(cap -> Optional.ofNullable(cap.getContainers().get(identifier)))
                .flatMap(container -> {
                    var stack = container.getAccessories().getItem(index);

                    if (stack.isEmpty()) return Optional.empty();

                    return Optional.of(new SlotResult(new SlotContext(identifier, capability.get().entity(), 0, false, true), stack));
                });
    }

    @Override
    public LivingEntity getWearer() {
        return this.capability().get().entity();
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
        this.capability().ifPresentOrElse(
                capability -> capability.addTransientSlotModifiers(modifiers),
                () -> LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!"));
    }

    @Override
    public void addPermanentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        this.capability().ifPresentOrElse(
                capability -> capability.addPersistentSlotModifiers(modifiers),
                () -> LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!"));
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        this.capability().ifPresentOrElse(
                capability -> capability.removeSlotModifiers(modifiers),
                () -> LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!"));
    }

    @Override
    public void clearSlotModifiers() {
        this.capability().ifPresentOrElse(
                capability -> capability.clearSlotModifiers(),
                () -> LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!"));
    }

    @Override
    public Multimap<String, AttributeModifier> getModifiers() {
        return this.capability().map(capability -> capability.getSlotModifiers()).orElseGet(() -> {
            LOGGER.warn("Unable to get the curios handlers from the given entity due to issues with getting the needed capability, expect errors!");

            return HashMultimap.create();
        });
    }

    @Override
    public ListTag saveInventory(boolean clear) {
        var outerCompound = new CompoundTag();

        var capability = this.capability();

        if (capability.isPresent()) {
            var compound = new CompoundTag();

            ((AccessoriesHolderImpl) capability.get().getHolder())
                    .write(new NbtMapCarrier(compound), SerializationContext.empty());

            outerCompound.put("main_data", compound);
            outerCompound.putBoolean("is_accessories_data", true);
        }

        var list = new ListTag();

        list.add(outerCompound);

        return list;
    }

    @Override
    public void loadInventory(ListTag data) {
        var compound = data.getCompound(0);

        try {
            var capability = this.capability();

            if (capability.isPresent()) {
                if (compound.contains("is_accessories_data")) {
                    ((AccessoriesHolderImpl) capability.get().getHolder())
                            .read(new NbtMapCarrier(compound), SerializationContext.empty());
                } else {
                    CurioInventory.readData(this.getWearer(), capability.get(), data);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load a wrapped curio inventory as a error occurred, it will not be loaded!", e);
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
        this.capability().ifPresent(AccessoriesCapabilityImpl::clearCachedSlotModifiers);
    }

    @Override
    public void growSlotType(String identifier, int amount) {
    }

    @Override
    public void shrinkSlotType(String identifier, int amount) {
    }
}
