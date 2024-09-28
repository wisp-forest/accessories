package dev.emi.trinkets.api;

public class TrinketEnums {

    public enum DropRule {
        KEEP, DROP, DESTROY, DEFAULT;

        static public boolean has(String name) {
            DropRule[] rules = DropRule.values();

            for (DropRule rule : rules) {
                if (rule.toString().equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static io.wispforest.accessories.api.DropRule convert(DropRule dropRule){
        return switch (dropRule){
            case KEEP -> io.wispforest.accessories.api.DropRule.KEEP;
            case DROP -> io.wispforest.accessories.api.DropRule.DROP;
            case DESTROY -> io.wispforest.accessories.api.DropRule.DESTROY;
            case DEFAULT -> io.wispforest.accessories.api.DropRule.DEFAULT;
        };
    }

    public static DropRule convert(io.wispforest.accessories.api.DropRule dropRule){
        return switch (dropRule){
            case KEEP -> DropRule.KEEP;
            case DROP -> DropRule.DROP;
            case DESTROY -> DropRule.DESTROY;
            case DEFAULT -> DropRule.DEFAULT;
        };
    }
}
