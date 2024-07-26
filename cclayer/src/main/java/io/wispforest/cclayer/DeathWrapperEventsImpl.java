package io.wispforest.cclayer;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDeathCallback;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeathWrapperEventsImpl implements OnDeathCallback, OnDropCallback {

    public static final DeathWrapperEventsImpl INSTANCE = new DeathWrapperEventsImpl();

    public static void init() {
        OnDeathCallback.EVENT.register(INSTANCE);
        OnDropCallback.EVENT.register(INSTANCE);
    }

    @Nullable
    private DropRulesEvent latestDropRules = null;

    @Override
    public TriState shouldDrop(TriState currentState, LivingEntity entity, AccessoriesCapability capability, DamageSource damageSource, List<ItemStack> droppedStacks) {
        var handler = new WrappedCurioItemHandler(() -> (AccessoriesCapabilityImpl) capability);

        var itemEntities = droppedStacks.stream()
                .map(stack -> {
                    var itemEntity = EntityType.ITEM.create(entity.level());

                    if(itemEntity == null) return null;

                    itemEntity.setItem(stack);

                    return itemEntity;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        droppedStacks.clear();

        var dropEventTest = new CurioDropsEvent(entity, handler, damageSource, itemEntities, 0, false);

        MinecraftForge.EVENT_BUS.post(dropEventTest);

        droppedStacks.addAll(itemEntities.stream().map(ItemEntity::getItem).toList());

        if(dropEventTest.isCanceled()) return TriState.FALSE;

        var event = new DropRulesEvent(entity, handler, damageSource, 0, false);

        MinecraftForge.EVENT_BUS.post(event);

        this.latestDropRules = event;

        return TriState.DEFAULT;
    }

    @Override
    public @Nullable DropRule onDrop(DropRule dropRule, ItemStack stack, SlotReference reference, DamageSource damageSource) {
        if(latestDropRules != null) {
            for (var override : latestDropRules.getOverrides()) {
                if (override.getA().test(stack)) return CuriosWrappingUtils.convert(override.getB());
            }
        }

        return null;
    }
}
