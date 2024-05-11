package io.wispforest.accessories;

public class DataLoaderImplBase {

    public static DataLoaderImplBase INSTANCE = new DataLoaderImplBase();

    public void registerListeners() {
        // NO-OP
        System.out.println("Registering Listeners");
    }
}
