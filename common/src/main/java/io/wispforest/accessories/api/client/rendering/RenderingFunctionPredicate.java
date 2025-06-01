package io.wispforest.accessories.api.client.rendering;

import com.google.common.base.CaseFormat;
import io.wispforest.accessories.pond.ModelRootAccess;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.LivingEntity;

public sealed interface RenderingFunctionPredicate permits RenderingFunctionPredicate.ModelTarget {

    Endec<RenderingFunctionPredicate> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "model_target" -> RenderingFunctionPredicate.ModelTarget.ENDEC;
                default -> throw new IllegalStateException("A invalid rendering function was created meaning such is unable to be decoded!");
            },
            RenderingFunctionPredicate::key,
            Endec.STRING,
            "type"
    );

    record ModelTarget(String modelPartName) implements RenderingFunctionPredicate {
        public static final StructEndec<ModelTarget> ENDEC = StructEndecBuilder.of(
                Endec.STRING.fieldOf("model_part", ModelTarget::modelPartName),
                ModelTarget::new
        );

        @Override
        public boolean shouldRender(LivingEntity entity, Model model) {
            if (model instanceof ModelRootAccess access) {
                return access.accessories$getAnyDescendantWithName(this.modelPartName()).isPresent();
            }

            return false;
        }
    }

    boolean shouldRender(LivingEntity entity, Model model);

    default String key() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName());
    }
}
