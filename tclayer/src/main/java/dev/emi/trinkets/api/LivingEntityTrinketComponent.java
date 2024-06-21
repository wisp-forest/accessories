package dev.emi.trinkets.api;

import dev.emi.trinkets.compat.WrappedTrinketComponent;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class LivingEntityTrinketComponent extends WrappedTrinketComponent implements AutoSyncedComponent {

    public LivingEntityTrinketComponent(AccessoriesCapability capability) {
        super(capability);
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
            for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
                TrinketInventory inv = slotType.getValue();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (predicate.test(inv.getItem(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        List<Tuple<SlotReference, ItemStack>> list = new ArrayList<>();
        forEach((slotReference, itemStack) -> {
            if (predicate.test(itemStack)) {
                list.add(new Tuple<>(slotReference, itemStack));
            }
        });
        return list;
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
        for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
            for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
                TrinketInventory inv = slotType.getValue();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    consumer.accept(new SlotReference(inv, i), inv.getItem(i));
                }
            }
        }
    }

    //--

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return false;
    }
}