package io.wispforest.accessories.api.action;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

///
/// Acts as a buffer to hold all responses for a given Action check.
///
/// If [#allowEarlyReturn] is found to be true, will attempt to return if any response is found to have a result
/// of [INVALID][ValidationState#INVALID] with the **requirement** being that outside callers must check [#shouldReturnEarly()]
/// to see if they need to break out of iteration loop.
///
public class ActionResponseBuffer {

    @Nullable
    private final ActionResponseBuffer parent;
    private final List<ActionResponse> responses = new ArrayList<>();

    private final boolean allowEarlyReturn;

    private boolean shouldReturnEarly = false;

    private ValidationState canPerformAction = ValidationState.IRRELEVANT;

    public ActionResponseBuffer(boolean allowEarlyReturn) {
        this(null, allowEarlyReturn);
    }

    private ActionResponseBuffer(ActionResponseBuffer parent, boolean allowEarlyReturn) {
        this.parent = parent;
        this.allowEarlyReturn = allowEarlyReturn;
    }

    //--

    public void respondWith(ActionResponse response) {
        responses.add(response);

        var value = response.canPerformAction();

        if (canPerformAction == value) return;

        if (value == ValidationState.INVALID) {
            canPerformAction = ValidationState.INVALID;

            if (allowEarlyReturn) shouldReturnEarly = true;
        } else if (value == ValidationState.VALID && canPerformAction == ValidationState.IRRELEVANT) {
            canPerformAction = ValidationState.VALID;
        }
    }

    ///
    /// @return If the given outer iteration loop should cancel early because of a result within the buffer
    ///
    public boolean shouldReturnEarly() {
        return shouldReturnEarly;
    }

    ///
    /// @return The final resultant of the buffers contents, as checked within [#respondWith(ActionResponse)],
    /// indicating either its [VALID][ValidationState#VALID], [INVALID][ValidationState#INVALID],
    /// or [IRRELEVANT][ValidationState#IRRELEVANT] to the outcome of the action check.
    ///
    public ValidationState canPerformAction() {
        return canPerformAction;
    }

    ///
    /// @return Whether the given buffer has any responses
    ///
    public boolean isEmpty() {
        return responses.isEmpty();
    }

    ///
    /// @return An immutable view of the responses within the buffer
    ///
    public List<ActionResponse> responses() {
        return Collections.unmodifiableList(responses);
    }

    ///
    /// @return An immutable filtered view of the responses within the buffer partially based on the
    /// buffers [#canPerformAction()] value
    ///
    public List<ActionResponse> responses(boolean filterValidStates) {
        return responses(filterValidStates, canPerformAction.isValid());
    }

    ///
    /// @return An immutable filtered view of the responses within the buffer.
    ///
    public List<ActionResponse> responses(boolean filterValidStates, boolean filterIrrelevantStates) {
        if (responses.isEmpty()) return List.of();

        var stream = responses.stream().filter(response -> {
            var value = response.canPerformAction();

            if (value == ValidationState.VALID && filterValidStates) return false;
            if (value == ValidationState.IRRELEVANT && filterIrrelevantStates) return false;

            return true;
        });

        if(!(filterValidStates && filterIrrelevantStates)) {
            stream = stream.sorted(Comparator.comparing(ActionResponse::canPerformAction));
        }

        return stream.toList();
    }
}
