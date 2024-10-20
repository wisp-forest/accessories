package io.wispforest.accessories.api.client;

import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static io.wispforest.accessories.api.client.Transformation.*;

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

    static Transformation translation(Vector3f translation) {
        return new Translation(translation);
    }

    static Transformation rawRotation(Quaternionf rotation) {
        return new RawRotation(rotation);
    }

    static Transformation axisRotation(AxisAngle4f rotation) {
        return new AxisRotation(rotation);
    }

    static Transformation scale(Vector3f scale) {
        return new Scale(scale);
    }

    static Transformation modelTarget(ModelTarget target) {
        return new TransformTo(target);
    }

    static Transformation matrix(Matrix4f matrix4f) {
        return new Matrix(matrix4f);
    }

    //--

    record Translation(Vector3f translation) implements Transformation {
        public static final StructEndec<Translation> ENDEC = StructEndecBuilder.of(EndecUtils.VECTOR_3_F_ENDEC.fieldOf("value", Translation::translation), Translation::new);

        @Override public String key() { return "translation"; }
    }

    record RawRotation(Quaternionf quarternionf) implements Transformation {
        public static final StructEndec<RawRotation> ENDEC = StructEndecBuilder.of(EndecUtils.QUATERNIONF_COMPONENTS.fieldOf("value", RawRotation::quarternionf), RawRotation::new);

        @Override public String key() { return "raw_rotation"; }
    }

    record AxisRotation(AxisAngle4f axisAngle4f) implements Transformation {
        public static final StructEndec<AxisRotation> ENDEC = StructEndecBuilder.of(EndecUtils.AXISANGLE4F.fieldOf("value", AxisRotation::axisAngle4f), AxisRotation::new);

        @Override public String key() { return "axis_rotation"; }
    }

    record Scale(Vector3f scale) implements Transformation {
        public static final StructEndec<Scale> ENDEC = StructEndecBuilder.of(EndecUtils.VECTOR_3_F_ENDEC.fieldOf("value", Scale::scale), Scale::new);

        @Override public String key() { return "scale"; }
    }

    record TransformTo(ModelTarget target) implements Transformation {
        public static final StructEndec<TransformTo> ENDEC = StructEndecBuilder.of(ModelTarget.ENDEC.fieldOf("value", TransformTo::target), TransformTo::new);

        @Override public String key() { return "transform_to"; }
    }

    record Matrix(Matrix4f matrix4f) implements Transformation {
        public static final StructEndec<Matrix> ENDEC = StructEndecBuilder.of(EndecUtils.MATRIX4F.fieldOf("value", Matrix::matrix4f), Matrix::new);

        @Override public String key() { return "matrix"; }
    }

    //--

    default String key() {
        throw new IllegalStateException("A invalid transform was created meaning a valid key was not found!");
    }
}
