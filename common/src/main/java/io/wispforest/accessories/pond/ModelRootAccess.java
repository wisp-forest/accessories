package io.wispforest.accessories.pond;

import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface ModelRootAccess {
    @Nullable
    default ModelPart accessories$rootPart(){
        throw new IllegalStateException("Interface Method not overridden!");
    }

    default Optional<ModelPart> getAnyDescendantWithName(String name) {
        var root = accessories$rootPart();

        if (root == null) return Optional.empty();
        if (name.equals("root")) return Optional.of(root);

        return getAnyDescendantWithName(root, name);
    }

    private static Optional<ModelPart> getAnyDescendantWithName(ModelPart part, String name) {
        for (var entry : ((ModelPartAccessor) (Object) part).getChildren().entrySet()) {
            var childName = entry.getKey();
            var childPart = entry.getValue();

            if (childName.equals(name)) return Optional.of(childPart);

            var result = getAnyDescendantWithName(part, name);

            if (result.isPresent()) return result;
        }

        return Optional.empty();
    }
}
