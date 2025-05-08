package io.wispforest.accessories.api.client.rendering;

import com.google.common.base.CaseFormat;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static io.wispforest.accessories.api.client.rendering.Transformation.*;

public sealed interface Transformation permits Translation, RawRotation, AxisRotation, Scale, TransformTo, Matrix {

    Endec<Transformation> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "translation" -> Translation.ENDEC;
                case "raw_rotation" -> RawRotation.ENDEC;
                case "axis_rotation" -> AxisRotation.ENDEC;
                case "scale" -> Scale.ENDEC;
                case "transform_to" -> TransformTo.ENDEC;
                case "matrix" -> Matrix.ENDEC;
                default -> throw new IllegalStateException("A invalid transform was created meaning such is unable to be decoded!");
            },
            Transformation::key,
            Endec.STRING,
            "type"
    );

    //--

    static Transformation translation(float x, float y, float z) {
        return translation(new Vector3f(x, y, z));
    }

    static Transformation translation(Vector3f translation) {
        return new Translation(translation);
    }

    static Transformation rawRotation(Quaternionf rotation) {
        return new RawRotation(rotation);
    }

    static Transformation axisRotation(float angle, Side side) {
        var axis = side.rotationAxis();
        return axisRotation(new AxisAngle4f(angle, axis.getX(), axis.getY(), axis.getZ()));
    }

    static Transformation axisRotation(float angle, float x, float y, float z) {
        return axisRotation(new AxisAngle4f(angle, x, y, z));
    }

    static Transformation axisRotation(AxisAngle4f rotation) {
        return new AxisRotation(rotation);
    }

    static Transformation scale(float x, float y, float z) {
        return scale(new Vector3f(x, y, z));
    }

    static Transformation scale(Vector3f scale) {
        return new Scale(scale);
    }

    static Transformation modelTarget(String modelPart, @Nullable Vector3f rawNormal) {
        return modelTarget(new ModelTarget(modelPart, rawNormal, null));
    }

    static Transformation modelTarget(String modelPart, @Nullable Side side) {
        return modelTarget(new ModelTarget(modelPart, null, side));
    }

    static Transformation modelTarget(ModelTarget target) {
        return new TransformTo(target);
    }

    static Transformation matrix(Matrix4f matrix4f) {
        return new Matrix(matrix4f);
    }

    //--

    record Translation(Vector3f translation) implements Transformation {
        public static final StructEndec<Translation> ENDEC = StructEndecBuilder.of(EndecUtils.VECTOR_3_F_ENDEC.flatFieldOf(Translation::translation), Translation::new);
    }

    record RawRotation(Quaternionf quarternionf) implements Transformation {
        public static final StructEndec<RawRotation> ENDEC = StructEndecBuilder.of(EndecUtils.QUATERNIONF_COMPONENTS.flatFieldOf(RawRotation::quarternionf), RawRotation::new);
    }

    record AxisRotation(AxisAngle4f axisAngle4f) implements Transformation {
        public static final StructEndec<AxisRotation> ENDEC = StructEndecBuilder.of(EndecUtils.AXISANGLE4F.flatFieldOf(AxisRotation::axisAngle4f), AxisRotation::new);
    }

    record Scale(Vector3f scale) implements Transformation {
        public static final StructEndec<Scale> ENDEC = StructEndecBuilder.of(EndecUtils.VECTOR_3_F_ENDEC.flatFieldOf(Scale::scale), Scale::new);
    }

    record TransformTo(ModelTarget target) implements Transformation {
        public static final StructEndec<TransformTo> ENDEC = StructEndecBuilder.of(ModelTarget.ENDEC.flatFieldOf(TransformTo::target), TransformTo::new);
    }

    record Matrix(Matrix4f matrix4f) implements Transformation {
        public static final StructEndec<Matrix> ENDEC = StructEndecBuilder.of(EndecUtils.MATRIX4F.fieldOf("value", Matrix::matrix4f), Matrix::new);
    }

    //--

    default String key() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName());
    }
}
