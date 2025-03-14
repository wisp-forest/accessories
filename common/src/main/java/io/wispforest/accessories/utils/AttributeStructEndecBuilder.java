package io.wispforest.accessories.utils;


import io.wispforest.endec.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class AttributeStructEndecBuilder<T> {

    private final Map<SerializationAttribute, StructEndec<T>> branches = new LinkedHashMap<>();

    public AttributeStructEndecBuilder(StructEndec<T> endec, SerializationAttribute attribute) {
        this.branches.put(attribute, endec);
    }

    public AttributeStructEndecBuilder<T> orElseIf(StructEndec<T> endec, SerializationAttribute attribute) {
        return orElseIf(attribute, endec);
    }

    public AttributeStructEndecBuilder<T> orElseIf(SerializationAttribute attribute, StructEndec<T> endec) {
        if (this.branches.containsKey(attribute)) {
            throw new IllegalStateException("Cannot have more than one branch for attribute " + attribute.name);
        }

        this.branches.put(attribute, endec);
        return this;
    }

    public StructEndec<T> orElse(StructEndec<T> endec) {
        return new StructEndec<T>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
                var branchEndec = endec;

                for (var branch : AttributeStructEndecBuilder.this.branches.entrySet()) {
                    if (ctx.hasAttribute(branch.getKey())) {
                        branchEndec = branch.getValue();
                        break;
                    }
                }

                branchEndec.encodeStruct(ctx, serializer, struct, value);
            }

            @Override
            public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                var branchEndec = endec;

                for (var branch : AttributeStructEndecBuilder.this.branches.entrySet()) {
                    if (ctx.hasAttribute(branch.getKey())) {
                        branchEndec = branch.getValue();
                        break;
                    }
                }

                return branchEndec.decodeStruct(ctx, deserializer, struct);
            }
        };
    }
}
