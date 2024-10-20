package io.wispforest.accessories.fabric.data;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.providers.entity.EntityBindingProvider;
import io.wispforest.accessories.api.data.providers.group.GroupDataProvider;
import io.wispforest.accessories.api.data.providers.slot.SlotDataProvider;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class AccessoriesDataGenEntrypoint implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        if (true) return;

        pack.addProvider((fabricDataOutput, completableFuture) -> {
            return new SlotDataProvider(fabricDataOutput, completableFuture) {
                @Override
                protected void buildData(HolderLookup.Provider provider, SlotOutput output) {
                    var modid = "accessories";

                    output.accept(
                            modid,
                            this.builder("test")
                                    .amount(1)
                                    .dropRule(DropRule.KEEP)
                                    .create()
                    );
                }
            };
        });

        pack.addProvider((fabricDataOutput, completableFuture) -> {
            return new GroupDataProvider(fabricDataOutput, completableFuture) {
                @Override
                protected void buildData(HolderLookup.Provider provider, GroupOutput output) {
                    var modid = "accessories";

                    output.accept(
                            modid,
                            this.builder("test")
                                    .slots("test", AccessoriesBaseData.ANKLET_SLOT)
                                    .icon(Accessories.of("idk"))
                                    .order(1)
                                    .create()
                    );
                }
            };
        });

        pack.addProvider((fabricDataOutput, completableFuture) -> {
            return new EntityBindingProvider(fabricDataOutput, completableFuture){
                @Override
                protected void buildData(HolderLookup.Provider provider, EntityBindingOutput output) {
                    var modid = "accessories";

                    output.accept(
                            ResourceLocation.fromNamespaceAndPath(modid, "test_binding"),
                            this.builder()
                                    .slots("test")
                                    .entityType(EntityType.PLAYER)
                                    .create()
                    );
                }
            };
        });
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        DataGeneratorEntrypoint.super.buildRegistry(registryBuilder);

        registryBuilder.add(ResourceKey.createRegistryKey(Accessories.of("fake_registry")), bootstrapContext -> {
            UniqueSlotHandling.gatherUniqueSlots((location, integer, resourceLocations) -> new SlotTypeReference(location.toString()));
        });
    }
}
