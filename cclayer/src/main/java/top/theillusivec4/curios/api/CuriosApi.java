/*
 * Copyright (c) 2018-2020 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;
import top.theillusivec4.curios.api.type.util.IIconHelper;
import top.theillusivec4.curios.api.type.util.ISlotHelper;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CuriosApi {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final String MODID = "curios";

  public static void registerCurio(Item item, ICurioItem curio) {
    CuriosImplMixinHooks.registerCurio(item, curio);
  }

  public static Optional<ISlotType> getSlot(String id, Level level) {
    return CuriosApi.getSlot(id, level.isClientSide());
  }

  public static Optional<ISlotType> getSlot(String id, boolean isClient) {
    return Optional.ofNullable(CuriosApi.getSlots(isClient).get(id));
  }

  public static Map<String, ISlotType> getSlots(Level level) {
    return CuriosApi.getSlots(level.isClientSide());
  }

  public static Map<String, ISlotType> getSlots(boolean isClient) {
    return CuriosImplMixinHooks.getSlots(isClient);
  }

  public static Map<String, ISlotType> getPlayerSlots(Level level) {
    return CuriosApi.getPlayerSlots(level.isClientSide());
  }

  public static Map<String, ISlotType> getPlayerSlots(boolean isClient) {
    return CuriosApi.getEntitySlots(EntityType.PLAYER, isClient);
  }

  public static Map<String, ISlotType> getEntitySlots(LivingEntity livingEntity) {
    return CuriosApi.getEntitySlots(livingEntity.getType(), livingEntity.level());
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, Level level) {
    return CuriosApi.getEntitySlots(type, level.isClientSide());
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    return CuriosImplMixinHooks.getEntitySlots(type, isClient);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, Level level) {
    return CuriosApi.getItemStackSlots(stack, level.isClientSide());
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
    return CuriosImplMixinHooks.getItemStackSlots(stack, isClient);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, LivingEntity livingEntity) {
    return CuriosImplMixinHooks.getItemStackSlots(stack, livingEntity);
  }

  public static Optional<ICurio> getCurio(ItemStack stack) {
    return CuriosImplMixinHooks.getCurio(stack);
  }

  public static Optional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {
    return CuriosImplMixinHooks.getCuriosInventory(livingEntity);
  }

  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    return CuriosImplMixinHooks.isStackValid(slotContext, stack);
  }

  public static Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    return CuriosImplMixinHooks.getAttributeModifiers(slotContext, id, stack);
  }

  public static Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
    return CuriosImplMixinHooks.getAttributeModifiers(slotContext, uuid, stack);
  }

  public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, String identifier, double amount, AttributeModifier.Operation operation) {
    CuriosImplMixinHooks.addSlotModifier(map, identifier, amount, operation);
  }

  public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, String identifier, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
    CuriosImplMixinHooks.addSlotModifier(map, identifier, id, amount, operation);
  }

  public static void addSlotModifier(ItemStack stack, String identifier, String name, double amount, AttributeModifier.Operation operation, String slot) {
    CuriosImplMixinHooks.addSlotModifier(stack, identifier, name, amount, operation, slot);
  }

  public static void addSlotModifier(ItemStack stack, String identifier, String name, ResourceLocation id, double amount, AttributeModifier.Operation operation, String slot) {
    CuriosImplMixinHooks.addSlotModifier(stack, identifier, id, amount, operation, slot);
  }

  public static ItemAttributeModifiers withSlotModifier(ItemAttributeModifiers itemAttributeModifiers, String identifier, ResourceLocation id, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup) {
    //return CuriosImplMixinHooks.withSlotModifier(itemAttributeModifiers, identifier, id, amount, operation, slotGroup);
    return ItemAttributeModifiers.EMPTY;
  }

  public static ItemAttributeModifiers withSlotModifier(ItemAttributeModifiers itemAttributeModifiers, String identifier, UUID uuid, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup) {
    //return CuriosImplMixinHooks.withSlotModifier(itemAttributeModifiers, identifier, uuid, amount, operation, slotGroup);
    return ItemAttributeModifiers.EMPTY;
  }

  public static void addModifier(ItemStack stack, Holder<Attribute> attribute, String name, double amount, AttributeModifier.Operation operation, String slot) {
    CuriosImplMixinHooks.addModifier(stack, attribute, name, amount, operation, slot);
  }

  public static void addModifier(ItemStack stack, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation, String slot) {
    CuriosImplMixinHooks.addModifier(stack, attribute, id, amount, operation, slot);
  }

  public static void registerCurioPredicate(ResourceLocation resourceLocation, Predicate<SlotResult> predicate) {
    CuriosImplMixinHooks.registerCurioPredicate(resourceLocation, predicate);
  }

  public static Optional<Predicate<SlotResult>> getCurioPredicate(ResourceLocation resourceLocation) {
    return CuriosImplMixinHooks.getCurioPredicate(resourceLocation);
  }

  public static Map<ResourceLocation, Predicate<SlotResult>> getCurioPredicates() {
    return CuriosImplMixinHooks.getCurioPredicates();
  }

  public static boolean testCurioPredicates(Set<ResourceLocation> predicates, SlotResult slotResult) {
    return CuriosImplMixinHooks.testCurioPredicates(predicates, slotResult);
  }

  public static UUID getSlotUuid(SlotContext slotContext) {
    return CuriosImplMixinHooks.getSlotUuid(slotContext);
  }

  public static ResourceLocation getSlotId(SlotContext slotContext) {
    return CuriosImplMixinHooks.getSlotId(slotContext);
  }

  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    CuriosImplMixinHooks.broadcastCurioBreakEvent(slotContext);
  }

  static void apiError() {
    LOGGER.error("Missing Curios API implementation!");
    for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
      LOGGER.error(stackTraceElement.toString());
    }
  }

  // ========= DEPRECATED =============

  /**
   * @deprecated See {@link CuriosApi#getSlot(String, Level)}
   * and {@link CuriosApi#getSlot(String, boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Optional<ISlotType> getSlot(String id) {
    return CuriosApi.getSlot(id, false);
  }

  /**
   * @deprecated See {@link CuriosApi#getSlot(String, Level)} and {@link ISlotType#getIcon()}.
   */
  @Nonnull
  public static ResourceLocation getSlotIcon(String id) {
    return CuriosApi.getSlot(id, true).map(ISlotType::getIcon)
            .orElse(ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot/empty_curio_slot"));
  }

  /**
   * @deprecated See {@link CuriosApi#getSlots(Level)} and {@link CuriosApi#getSlots(boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getSlots() {
    return CuriosApi.getSlots(false);
  }

  /**
   * @deprecated See {@link CuriosApi#getEntitySlots(EntityType, Level)},
   * {@link CuriosApi#getEntitySlots(EntityType, boolean)},
   * and {@link CuriosApi#getEntitySlots(LivingEntity)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type) {
    return CuriosApi.getEntitySlots(type, false);
  }

  /**
   * @deprecated See {@link CuriosApi#getPlayerSlots(Level)},
   * {@link CuriosApi#getPlayerSlots(boolean)}, and {@link CuriosApi#getPlayerSlots(Player)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getPlayerSlots() {
    return CuriosApi.getPlayerSlots(false);
  }

  /**
   * @deprecated See {@link CuriosApi#getItemStackSlots(ItemStack, Level)}
   * and {@link CuriosApi#getItemStackSlots(ItemStack, boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack) {
    return CuriosApi.getItemStackSlots(stack, FMLLoader.getDist() == Dist.CLIENT);
  }

  private static IIconHelper iconHelper;

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setIconHelper(IIconHelper helper) {

    if (iconHelper == null) {
      iconHelper = helper;
    }
  }

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static IIconHelper getIconHelper() {
    return iconHelper;
  }

  private static ICuriosHelper curiosHelper;

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setCuriosHelper(ICuriosHelper helper) {

    if (curiosHelper == null) {
      curiosHelper = helper;
    }
  }

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static ICuriosHelper getCuriosHelper() {
    return curiosHelper;
  }

  private static ISlotHelper slotHelper;

  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static ISlotHelper getSlotHelper() {
    return slotHelper;
  }

  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setSlotHelper(ISlotHelper helper) {
    slotHelper = helper;
  }
}
