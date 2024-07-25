//package io.wispforest.accessories.endec;
//
//import io.wispforest.accessories.mixin.HolderLookupAdapterAccessor;
//import io.wispforest.endec.SerializationAttribute;
//import net.minecraft.core.Registry;
//import net.minecraft.core.RegistryAccess;
//import net.minecraft.resources.RegistryOps;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.resources.ResourceLocation;
//import org.jetbrains.annotations.ApiStatus;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Function;
//
//public class RegistriesAttribute implements SerializationAttribute.Instance {
//
//    public static final SerializationAttribute.WithValue<RegistriesAttribute> REGISTRIES = SerializationAttribute.withValue("registries");
//
//    private final RegistryOps.RegistryInfoLookup infoGetter;
//    private final @Nullable RegistryAccess registryManager;
//
//    private RegistriesAttribute(RegistryOps.RegistryInfoLookup infoGetter, @Nullable RegistryAccess registryManager) {
//        this.infoGetter = infoGetter;
//        this.registryManager = registryManager;
//    }
//
//    public static RegistriesAttribute of(RegistryAccess registryManager) {
//        return new RegistriesAttribute(
//                new RegistryOps.HolderLookupAdapter(registryManager),
//                registryManager
//        );
//    }
//
//    @ApiStatus.Internal
//    public static RegistriesAttribute infoGetterOnly(RegistryOps.RegistryInfoLookup lookup) {
//        RegistryAccess registryManager = null;
//
//        if(lookup instanceof RegistryOps.HolderLookupAdapter holderLookupAdapter && ((HolderLookupAdapterAccessor) holderLookupAdapter).getLookupProvider() instanceof RegistryAccess registryAccess) {
//            registryManager = registryAccess;
//        }
//
//        return new RegistriesAttribute(lookup, registryManager);
//    }
//
//    public RegistryOps.RegistryInfoLookup infoGetter() {
//        return this.infoGetter;
//    }
//
//    public boolean hasRegistryManager() {
//        return this.registryManager != null;
//    }
//
//    public @NotNull RegistryAccess registryManager() {
//        if (!this.hasRegistryManager()) {
//            throw new IllegalStateException("This instance of RegistriesAttribute does not supply a RegistryAccess");
//        }
//
//        return this.registryManager;
//    }
//
//    @Override
//    public SerializationAttribute attribute() {
//        return REGISTRIES;
//    }
//
//    @Override
//    public Object value() {
//        return this;
//    }
//}
