package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.core.PositionedRectangle;

import java.util.List;

public record CollectedPositionedRectangle(PositionedRectangle primaryCheck, PositionedRectangle[] secondaryChecks) implements PositionedRectangle {

    public static CollectedPositionedRectangle of(PositionedRectangle primaryCheck, List<PositionedRectangle> secondaryChecks) {
        if(secondaryChecks.size() == 1) {
            primaryCheck = secondaryChecks.getFirst();
            secondaryChecks = List.of();
        }

        return new CollectedPositionedRectangle(primaryCheck, secondaryChecks.toArray(PositionedRectangle[]::new));
    }

    public boolean isEmpty() {
        return secondaryChecks.length == 0;
    }

    @Override
    public int x() {
        return 0;
    }

    @Override
    public int y() {
        return 0;
    }

    @Override
    public int width() {
        return 0;
    }

    @Override
    public int height() {
        return 0;
    }

    @Override
    public boolean isInBoundingBox(double x, double y) {
        if(isInBoundingBox(primaryCheck, x, y)) {
            if (secondaryChecks.length == 0) return true;

            for (var secondaryCheck : secondaryChecks) {
                if (secondaryCheck.isInBoundingBox(x, y)) return true;
            }
        }

        return false;
    }

    private static boolean isInBoundingBox(PositionedRectangle rectangle, double x, double y) {
        return x >= rectangle.x() && x < rectangle.x() + rectangle.width() && y >= rectangle.y() && y < rectangle.y() + rectangle.height();
    }
}
