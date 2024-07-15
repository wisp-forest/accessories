package io.wispforest.accessories.pond.owo;

import io.wispforest.accessories.client.gui.utils.AbstractPolygon;
import io.wispforest.accessories.client.gui.utils.ComponentAsPolygon;
import io.wispforest.owo.ui.core.Component;

import java.util.Arrays;
import java.util.List;

public interface InclusiveBoundingArea<T extends Component> {

    default <P extends AbstractPolygon> T addInclusionZone(Component... components){
        return addInclusionZone(Arrays.stream(components).map(ComponentAsPolygon::new).toArray(AbstractPolygon[]::new));
    }

    <P extends AbstractPolygon> T addInclusionZone(P... polygon);

    <P extends AbstractPolygon> T addInclusionZone(List<P> polygons);

    List<AbstractPolygon> getInclusionZones();

    default boolean isWithinInclusionZone(float x, float y){
        for(AbstractPolygon polygon : getInclusionZones()){
            if(polygon.withinShape(x, y)) return true;
        }

        return false;
    }
}
