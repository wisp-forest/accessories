package io.wispforest.accessories.mixin.owo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInputContextHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueInput.class)
public interface TagValueInputAccessor {
    @Accessor("problemReporter") ProblemReporter accessories$problemReporter();

    @Accessor("input") CompoundTag accessories$input();

    @Accessor("context") ValueInputContextHelper accessories$context();
}
