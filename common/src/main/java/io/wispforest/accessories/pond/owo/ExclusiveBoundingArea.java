package io.wispforest.accessories.pond.owo;

import io.wispforest.accessories.client.gui.utils.AbstractPolygon;
import io.wispforest.accessories.client.gui.utils.ComponentAsPolygon;
import io.wispforest.owo.ui.core.Component;

import java.util.Arrays;
import java.util.List;

public interface ExclusiveBoundingArea<T extends Component> {

    default <P extends AbstractPolygon> T addExclusionZone(Component... components){
        return addExclusionZone(Arrays.stream(components).map(ComponentAsPolygon::new).toArray(AbstractPolygon[]::new));
    }

    <P extends AbstractPolygon> T addExclusionZone(P... polygon);

    <P extends AbstractPolygon> T addExclusionZone(List<P> polygons);

    List<AbstractPolygon> getExclusionZones();

    default boolean isWithinExclusionZone(float x, float y){
        for(var polygon : getExclusionZones()){
            if(polygon.withinShape(x, y)) return true;
        }

        return false;
    }
}
