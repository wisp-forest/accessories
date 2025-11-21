package io.wispforest.accessories.api.action;

import net.fabricmc.fabric.api.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionResponseBuffer {

    @Nullable
    private final ActionResponseBuffer parent;
    private final List<ActionResponse> responses = new ArrayList<>();

    private final boolean allowEarlyReturn;

    private boolean shouldReturnEarly = false;

    public ActionResponseBuffer(boolean allowEarlyReturn) {
        this(null, allowEarlyReturn);
    }

    private ActionResponseBuffer(ActionResponseBuffer parent, boolean allowEarlyReturn) {
        this.parent = parent;
        this.allowEarlyReturn = allowEarlyReturn;
    }

    public void respondWith(ActionResponse response) {
        responses.add(response);

        if (allowEarlyReturn && !response.canPerformAction()) {
            shouldReturnEarly = true;
        }
    }

    public boolean allowEarlyReturn() {
        return allowEarlyReturn;
    }

    public boolean shouldReturnEarly() {
        return shouldReturnEarly;
    }

    public List<ActionResponse> responses() {
        return Collections.unmodifiableList(responses);
    }

    public TriState canPerformAction() {
        if (responses.isEmpty()) return TriState.DEFAULT;

        return TriState.of(CompoundResponse.canPerformAction(responses));
    }

    public ActionResponseBuffer createInnerBuffer() {
        return this.allowEarlyReturn()
            ? this
            : new ActionResponseBuffer(this, false);
    }

    public boolean addToParentBuffer() {
        if (parent == null) return false;

        parent.respondWith(ActionResponse.combineToSingleResponse(this.responses()));

        return parent.shouldReturnEarly();
    }
}
