package io.wispforest.accessories.api.client.rendering;

import com.google.common.base.CaseFormat;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

//@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public sealed interface RenderingPredicate permits RenderingPredicate.ModelTarget {

    Endec<RenderingPredicate> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "model_target" -> RenderingPredicate.ModelTarget.ENDEC;
                default -> throw new IllegalStateException("A invalid rendering function was created meaning such is unable to be decoded!");
            },
            RenderingPredicate::key,
            Endec.STRING,
            "type"
    );

    record ModelTarget(String modelPartName) implements RenderingPredicate {
        public static final StructEndec<ModelTarget> ENDEC = StructEndecBuilder.of(
                Endec.STRING.fieldOf("model_part", ModelTarget::modelPartName),
                ModelTarget::new
        );

        @Override
        public boolean shouldRender(LivingEntity entity, Model model) {
            return ModelTransformOps.getPart(model, this.modelPartName()) != null;
        }
    }

    boolean shouldRender(LivingEntity entity, Model model);

    default String key() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName());
    }
}
