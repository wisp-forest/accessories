package dev.emi.trinkets.api;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SlotType {
        private final String group;
        private final String name;
        private final int order;
        private final int amount;
        private final ResourceLocation icon;
        private final Set<ResourceLocation> quickMovePredicates;
        private final Set<ResourceLocation> validatorPredicates;
        private final Set<ResourceLocation> tooltipPredicates;
        private final TrinketEnums.DropRule dropRule;

        public SlotType(String group, String name, int order, int amount, ResourceLocation icon, Set<ResourceLocation> quickMovePredicates,
                        Set<ResourceLocation> validatorPredicates, Set<ResourceLocation> tooltipPredicates, TrinketEnums.DropRule dropRule) {
            this.group = group;
            this.name = name;
            this.order = order;
            this.amount = amount;
            this.icon = icon;
            this.quickMovePredicates = quickMovePredicates;
            this.validatorPredicates = validatorPredicates;
            this.tooltipPredicates = tooltipPredicates;
            this.dropRule = dropRule;
        }

        public String getGroup() {
            return group;
        }

        public String getName() {
            return name;
        }

        public int getOrder() {
            return order;
        }

        public int getAmount() {
            return amount;
        }

        public ResourceLocation getIcon() {
            return icon;
        }

        public Set<ResourceLocation> getQuickMovePredicates() {
            return quickMovePredicates;
        }

        public Set<ResourceLocation> getValidatorPredicates() {
            return validatorPredicates;
        }

        public Set<ResourceLocation> getTooltipPredicates() {
            return tooltipPredicates;
        }

        public TrinketEnums.DropRule getDropRule() {
            return dropRule;
        }

        public MutableComponent getTranslation() {
            return Component.translatable("trinkets.slot." + this.group + "." + this.name);
        }

        public void write(CompoundTag data) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Group", group);
            tag.putString("Name", name);
            tag.putInt("Order", order);
            tag.putInt("Amount", amount);
            tag.putString("Icon", icon.toString());
            ListTag quickMovePredicateList = new ListTag();

            for (ResourceLocation id : quickMovePredicates) {
                quickMovePredicateList.add(StringTag.valueOf(id.toString()));
            }
            tag.put("QuickMovePredicates", quickMovePredicateList);

            ListTag validatorPredicateList = new ListTag();

            for (ResourceLocation id : validatorPredicates) {
                validatorPredicateList.add(StringTag.valueOf(id.toString()));
            }
            tag.put("ValidatorPredicates", validatorPredicateList);

            ListTag tooltipPredicateList = new ListTag();

            for (ResourceLocation id : tooltipPredicates) {
                tooltipPredicateList.add(StringTag.valueOf(id.toString()));
            }
            tag.put("TooltipPredicates", tooltipPredicateList);
            tag.putString("DropRule", dropRule.toString());
            data.put("SlotData", tag);
        }

        public static SlotType read(CompoundTag data) {
            CompoundTag slotData = data.getCompound("SlotData");
            String group = slotData.getString("Group");
            String name = slotData.getString("Name");
            int order = slotData.getInt("Order");
            int amount = slotData.getInt("Amount");
            ResourceLocation icon = ResourceLocation.tryParse(slotData.getString("Icon"));
            ListTag quickMoveList = slotData.getList("QuickMovePredicates", NbtType.STRING);
            Set<ResourceLocation> quickMovePredicates = new HashSet<>();

            for (Tag tag : quickMoveList) {
                quickMovePredicates.add(ResourceLocation.tryParse(tag.getAsString()));
            }
            ListTag validatorList = slotData.getList("ValidatorPredicates", NbtType.STRING);
            Set<ResourceLocation> validatorPredicates = new HashSet<>();

            for (Tag tag : validatorList) {
                validatorPredicates.add(ResourceLocation.tryParse(tag.getAsString()));
            }
            ListTag tooltipList = slotData.getList("TooltipPredicates", NbtType.STRING);
            Set<ResourceLocation> tooltipPredicates = new HashSet<>();

            for (Tag tag : tooltipList) {
                tooltipPredicates.add(ResourceLocation.tryParse(tag.getAsString()));
            }
            String dropRuleName = slotData.getString("DropRule");
            TrinketEnums.DropRule dropRule = TrinketEnums.DropRule.DEFAULT;

            if (TrinketEnums.DropRule.has(dropRuleName)) {
                dropRule = TrinketEnums.DropRule.valueOf(dropRuleName);
            }
            return new SlotType(group, name, order, amount, icon, quickMovePredicates, validatorPredicates, tooltipPredicates, dropRule);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SlotType slotType = (SlotType) o;
            return group.equals(slotType.group) && name.equals(slotType.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, name);
        }

        public String getId() {
            return this.group + "/" + this.name;
        }
}
