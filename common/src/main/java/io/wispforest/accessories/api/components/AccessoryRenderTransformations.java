package io.wispforest.accessories.api.components;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.api.client.ModelTarget;
import io.wispforest.accessories.api.client.Transformation;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record AccessoryRenderTransformations(boolean disableDefaultTranslations, List<Transformation> transformations) {

    public static final AccessoryRenderTransformations EMPTY = new AccessoryRenderTransformations(false, List.of());

    public static final Endec<AccessoryRenderTransformations> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.optionalFieldOf("disable_default_translations", AccessoryRenderTransformations::disableDefaultTranslations, false),
            Transformation.ENDEC.listOf().fieldOf("transformations", AccessoryRenderTransformations::transformations),
            AccessoryRenderTransformations::new
    );

    public static Builder builder(){
        return new Builder();
    }

    public static AccessoryRenderTransformations builder(Function<Builder, AccessoryRenderTransformations> builderFunc){
        return builderFunc.apply(new Builder());
    }

    public static final class Builder {
        public List<Transformation> transformations = new ArrayList<>();

        private Builder() {}

        public Builder transformations(Transformation ...transformations) {
            return transformations(List.of(transformations));
        }

        public Builder transformations(List<Transformation> transformations) {
            this.transformations.addAll(transformations);

            return this;
        }

        public Builder translation(Vector3f translation) {
            this.transformations.add(Transformation.translation(translation));

            return this;
        }

        public Builder rotation(Quaternionf rotation) {
            this.transformations.add(Transformation.rawRotation(rotation));

            return this;
        }

        public Builder rotation(AxisAngle4f rotation) {
            this.transformations.add(Transformation.axisRotation(rotation));

            return this;
        }

        public Builder scale(Vector3f scale) {
            this.transformations.add(Transformation.scale(scale));

            return this;
        }

        public Builder modelTarget(ModelTarget target) {
            this.transformations.add(Transformation.modelTarget(target));

            return this;
        }

        public Builder matrix4f(Matrix4f matrix4f) {
            this.transformations.add(Transformation.matrix(matrix4f));

            return this;
        }

        public AccessoryRenderTransformations build() {
            return build(false);
        }

        public AccessoryRenderTransformations build(boolean disableDefaultTranslations) {
            return new AccessoryRenderTransformations(disableDefaultTranslations, ImmutableList.copyOf(transformations));
        }
    }
}
