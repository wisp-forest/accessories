package io.wispforest.accessories.mixin.owo;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueOutput.class)
public interface TagValueOutputAccessor  {
    @Accessor("problemReporter") ProblemReporter accessories$problemReporter();

    @Accessor("output") CompoundTag accessories$output();

    @Accessor("ops") DynamicOps<Tag> accessories$ops();
}
