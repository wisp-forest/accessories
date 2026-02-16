package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import io.wispforest.accessories.api.events.extra.OnTotemActivate;
import io.wispforest.accessories.api.events.extra.OnTotemConsumption;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.core.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements AccessoriesAPIAccess, AccessoriesLivingEntityExtension {

    @Unique
    private AccessoriesCapabilityImpl accessories$capability = null;

    @Unique
    private final Map<ItemStack, SlotReference> accessories$enchantmentLocationContext = new Reference2ObjectOpenHashMap<>();

    @Unique
    private final Map<String, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> accessories$activeLocationDependentEnchantments = new HashMap<>();

    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public AccessoriesCapability getOrCreateAccessoriesCapability() {
        if (accessories$capability == null) {
            this.accessories$capability = new AccessoriesCapabilityImpl((LivingEntity) (Object) this);
        }

        return accessories$capability;
    }

    @Override
    @Nullable
    public AccessoriesCapability accessoriesCapability() {
        var slots = EntitySlotLoader.getEntitySlots((LivingEntity) (Object) this);

        if(slots.isEmpty()) return null;

        var capability = getOrCreateAccessoriesCapability();

        // Used to init some functions behind the scene
        AccessoriesHolderImpl.getHolder(capability);

        return capability;
    }

    @Override
    public void pushEnchantmentContext(ItemStack stack, SlotReference reference) {
        this.accessories$enchantmentLocationContext.put(stack, reference);
    }

    @Override
    @Nullable
    public SlotReference popEnchantmentContext(ItemStack stack) {
        return this.accessories$enchantmentLocationContext.remove(stack);
    }

    @Override
    public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantmentsFromSlotReference(SlotReference slotReference) {
        return accessories$activeLocationDependentEnchantments.computeIfAbsent(slotReference.createString(), equipmentSlot -> new Reference2ObjectArrayMap());
    }

    //--

    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"), cancellable = true)
    private void sendAccessoriesBreakInstead(Item item, EquipmentSlot slot, CallbackInfo ci){
        if(slot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) ci.cancel();
    }

    @Inject(method = "entityEventForEquipmentBreak", at = @At("HEAD"), cancellable = true)
    private static void preventMatchExceptionForAccessories(EquipmentSlot slot, CallbackInfoReturnable<Byte> cir) {
        if(slot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) cir.setReturnValue((byte) -1);
    }

    public void onEquipItem(SlotReference slotReference, ItemStack oldItem, ItemStack newItem) {
        var level = this.level();

        if (!ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick && !level.isClientSide() && !this.isSpectator()) {
            var isEquitableFor = newItem.isEmpty() || SlotValidatorRegistry.canInsertIntoSlot(newItem, slotReference);

            if (!this.isSilent() && !newItem.isEmpty()) {
                var sound = AccessoryRegistry.getAccessoryOrDefault(newItem).getEquipSound(newItem, slotReference);

                if(sound != null) level.playSeededSound(null, this.getX(), this.getY(), this.getZ(), sound.event().value(), this.getSoundSource(), sound.volume(), sound.pitch(), this.random.nextLong());
            }

            if (isEquitableFor) this.gameEvent(!newItem.isEmpty() ? GameEvent.EQUIP : GameEvent.UNEQUIP);
        }
    }

    //--

    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void accessories$isGazeDisguised(LivingEntity livingEntity, double tolerance, boolean scaleByDistance, boolean visual, double[] yValues, CallbackInfoReturnable<Boolean> cir) {
        var state = ExtraEventHandler.isGazedBlocked((LivingEntity) (Object) this, livingEntity);

        if (state != TriState.DEFAULT) cir.setReturnValue(!state.get());
    }

    //--

    @ModifyReturnValue(method = "canFreeze", at = @At(value = "RETURN", ordinal = 1))
    private boolean canFreezeAccessoriesCheck(boolean bl) {
        var state = ExtraEventHandler.canFreezeEntity((LivingEntity) (Object) this);

        return state.orElse(bl);
    }

    //--

    @Inject(method = "checkTotemDeathProtection", at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 1, shift = At.Shift.BEFORE))
    private void accessories$checkForTotems(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) LocalRef<ItemStack> itemStack, @Local(ordinal = 0) LocalRef<DeathProtection> deathProtection, @Share(value = "currentSlotReference") LocalRef<@Nullable SlotReference> currentSlotReference) {
        var capability = this.accessoriesCapability();

        SlotReference slotReference = null;

        if (capability != null && deathProtection.get() == null) {
            var totem = capability.getFirstEquipped(ItemStackBasedPredicate.ofComponents("totem_check", DataComponents.DEATH_PROTECTION));

            if (totem != null) {
                slotReference = totem.reference();

                var totemStack = totem.stack();

                itemStack.set(totemStack.copy());
                deathProtection.set(totemStack.get(DataComponents.DEATH_PROTECTION));
                // TODO: SHOULD BE CONFIGURABLE IF SUCH GETS CONSUMED?

                var accessory = AccessoryRegistry.getAccessoryOrDefault(totemStack);

                var consumptionAction = (accessory instanceof OnTotemConsumption onTotemConsumption)
                        ? onTotemConsumption
                        : OnTotemConsumption.DEFAULT_BEHAVIOR;

                totemStack = consumptionAction.onConsumption(slotReference, totemStack, damageSource);

                slotReference.setStack(totemStack);
            }
        }

        currentSlotReference.set(slotReference);
    }

    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/DeathProtection;applyEffects(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void accessories$adjustTotemEffects(DeathProtection instance, ItemStack itemStack, LivingEntity livingEntity, Operation<Void> original, @Local(argsOnly = true) DamageSource damageSource, @Share(value = "currentSlotReference") LocalRef<@Nullable SlotReference> currentSlotReference) {
        var slotReference = currentSlotReference.get();

        if (slotReference != null) {
            var accessory = AccessoryRegistry.getAccessoryOrDefault(itemStack);

            var activationAction = (accessory instanceof OnTotemActivate onTotemActivate)
                    ? onTotemActivate
                    : OnTotemActivate.DEFAULT_BEHAVIOR;

            instance = activationAction.onActivation(instance, slotReference, itemStack, damageSource);

            if (instance == null) return;
        }

        original.call(instance, itemStack, livingEntity);
    }

    //--

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private List<EquipmentSlot> accessories$addEquipmentCheck(Stream<EquipmentSlot> instance, Operation<List<EquipmentSlot>> original, @Share("slotReference") LocalRef<@Nullable SlotReference> slotReference) {
        var capability = this.accessoriesCapability();

        slotReference.set(null);

        if (capability != null) {
            var gliders = capability.getEquipped(ItemStackBasedPredicate.ofComponents(DataComponents.GLIDER));

            if (!gliders.isEmpty()) {
                var glider = Util.getRandom(gliders, this.random);

                if (LivingEntity.canGlideUsing(glider.stack(), AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
                    slotReference.set(glider.reference());

                    instance = Stream.concat(instance, Stream.of(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot()));
                }
            }
        }

        return original.call(instance);
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack accessories$adjustGottenStack(LivingEntity instance, EquipmentSlot equipmentSlot, Operation<ItemStack> original, @Share("slotReference") LocalRef<@Nullable SlotReference> slotReference) {
        if (equipmentSlot != AccessoriesInternals.INSTANCE.getInternalEquipmentSlot()) return original.call(instance, equipmentSlot);

        var stack = slotReference.get().getStack();

        return stack != null ? stack : ItemStack.EMPTY;
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void accessories$adjustHurtAndBreak(ItemStack instance, int amount, LivingEntity entity, EquipmentSlot slot, Operation<Void> original, @Share("slotReference") LocalRef<@Nullable SlotReference> slotReference) {
        var ref = slotReference.get();

        if (ref == null) {
            original.call(instance, amount, entity, slot);
        } else if(entity.level() instanceof ServerLevel serverLevel) {
            instance.hurtAndBreak(amount, serverLevel, entity instanceof ServerPlayer serverPlayer ? serverPlayer : null, item -> ref.breakStack());
        }
    }

    @Inject(method = "canGlide", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EquipmentSlot;VALUES:Ljava/util/List;"), cancellable = true)
    private void accessories$checkAccessoriesGliders(CallbackInfoReturnable<Boolean> cir) {
        var capability = this.accessoriesCapability();

        if (capability == null) return;

        var gliders = capability.getEquipped(ItemStackBasedPredicate.ofComponents(DataComponents.GLIDER));

        if (gliders.isEmpty()) return;

        for (var glider : gliders) {
            if (LivingEntity.canGlideUsing(glider.stack(), AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
                cir.setReturnValue(true);
            }
        }
    }

    @WrapOperation(method = "canGlideUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/equipment/Equippable;slot()Lnet/minecraft/world/entity/EquipmentSlot;"))
    private static EquipmentSlot accessories$changeEquipmentSlot(Equippable instance, Operation<EquipmentSlot> original, @Local(argsOnly = true) EquipmentSlot equipmentSlot) {
        return (equipmentSlot == AccessoriesInternals.INSTANCE.getInternalEquipmentSlot()) ? AccessoriesInternals.INSTANCE.getInternalEquipmentSlot() : original.call(instance);
    }
}
