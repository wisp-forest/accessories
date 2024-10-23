package io.wispforest.accessories.api.slot;

import io.netty.buffer.ByteBuf;
import io.wispforest.accessories.impl.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.impl.slot.SlotReferenceImpl;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

/**
 * Helper class allowing for a Serialization of a given SlotReference instance across the network
 */
public class SlotReferenceEncoding {

    /**
     * Encodes the given {@link SlotReference} into the passed {@link ByteBuf} and returns such
     */
    public static ByteBuf encodeReference(ByteBuf byteBuf, SlotReference slotReference) {
        ENDEC.encode(SerializationContext.empty(), ByteBufSerializer.of(byteBuf), slotReference);

        return byteBuf;
    }

    /**
     * Safe method of decoding {@link SlotReference} data from the given {@link ByteBuf} requiring the {@link Level} that the
     * entity is located within as a parameter. It is recommended to double-check that the given {@link SlotReference}
     * is still valid using {@link SlotReference#isValid()} as changes may have occurred that invalidates the reference.
     */
    public static SlotReference decodeReference(ByteBuf byteBuf, Level level) {
        return ENDEC.decode(SerializationContext.attributes(new LevelAttribute(level)), ByteBufDeserializer.of(byteBuf));
    }

    //--

    private static final Endec<LivingEntity> LIVING_ENTITY_ENDEC = Endec.VAR_INT.xmapWithContext(
            (ctx, id) -> {
                var level = ctx.requireAttributeValue(LevelAttribute.LEVEL).level();
                var entity = level.getEntity(id);

                if(entity == null) {
                    throw new IllegalStateException("Unable to locate the given entity with the following ID with the passed level! [Id: " + id + " , Level: " + level.dimension() + " ]");
                }

                if(!(entity instanceof LivingEntity living)) {
                    throw new IllegalStateException("Given entity found within the world was not of LivingEntity! [Id: " + id + ", EntityType: " + entity.getType() + ", Level: " + level.dimension() + " ]");
                }

                return living;
            },
            (context, entity) -> entity.getId());

    private static final StructEndec<NestedSlotReferenceImpl> NESTED_SLOT_REFERENCE_ENDEC = StructEndecBuilder.of(
            LIVING_ENTITY_ENDEC.fieldOf("entity", NestedSlotReferenceImpl::entity),
            Endec.STRING.fieldOf("slotName", NestedSlotReferenceImpl::slotName),
            Endec.VAR_INT.fieldOf("initialHolderSlot", NestedSlotReferenceImpl::initialHolderSlot),
            Endec.VAR_INT.listOf().fieldOf("innerSlotIndices", NestedSlotReferenceImpl::innerSlotIndices),
            NestedSlotReferenceImpl::new);

    private static final StructEndec<SlotReferenceImpl> BASE_SLOT_REFERENCE_ENDEC = StructEndecBuilder.of(
            LIVING_ENTITY_ENDEC.fieldOf("entity", SlotReferenceImpl::entity),
            Endec.STRING.fieldOf("slotName", SlotReferenceImpl::slotName),
            Endec.VAR_INT.fieldOf("slot", SlotReferenceImpl::slot),
            SlotReferenceImpl::new);

    /**
     * An {@link Endec} for {@link SlotReference} that requires during {@link Endec#encode} or {@link Endec#decode}  that
     * the given {@link SerializationContext} passed within the given method calls requires a
     * {@link LevelAttribute} to properly en(de)code the given reference data
     */
    @ApiStatus.Experimental
    public static final Endec<SlotReference> ENDEC = Endec.dispatchedStruct(
            key -> switch (key) {
                case "nested" -> NESTED_SLOT_REFERENCE_ENDEC;
                case "base" -> BASE_SLOT_REFERENCE_ENDEC;
                default -> throw new IllegalStateException("Unable to find endec for the given SlotReference type: " + key);
            }, slotReference -> switch (slotReference) {
                case NestedSlotReferenceImpl ignored -> "nested";
                case SlotReferenceImpl ignored -> "base";
                default -> throw new IllegalStateException("Unable to handle the given SlotReference type: " + slotReference.getClass().getSimpleName());
            }, Endec.STRING);

    @ApiStatus.Experimental
    public record LevelAttribute(Level level) implements SerializationAttribute.Instance {
        public static final SerializationAttribute.WithValue<LevelAttribute> LEVEL = SerializationAttribute.withValue("current_minecraft_level");

        @Override public SerializationAttribute attribute() { return LEVEL; }
        @Override public Object value() { return this; }
    }
}
