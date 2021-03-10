/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;

/**
 * Describes the evaluation state of assignment / object condition.
 */
@Experimental
public class ConditionState implements Serializable {

    /**
     * State of the condition corresponding to old state of the focal object.
     */
    private final boolean oldState;

    /**
     * State of the condition corresponding to current state of the focal object.
     */
    private final boolean currentState;

    /**
     * State of the condition corresponding to new state of the focal object.
     */
    private final boolean newState;

    private ConditionState(boolean oldState, boolean currentState, boolean newState) {
        this.oldState = oldState;
        this.currentState = currentState;
        this.newState = newState;
    }

    public static ConditionState merge(ConditionState state1, ConditionState state2) {
        return new ConditionState(state1.oldState && state2.oldState,
                state1.currentState && state2.currentState,
                state1.newState && state2.newState);
    }

    public static ConditionState allTrue() {
        return new ConditionState(true, true, true);
    }

    public static ConditionState from(boolean oldState, boolean currentState, boolean newState) {
        return new ConditionState(oldState, currentState, newState);
    }

    public boolean isOldTrue() {
        return oldState;
    }

    public boolean isCurrentTrue() {
        return currentState;
    }

    public boolean isNewTrue() {
        return newState;
    }

    public boolean isNewFalse() {
        return !newState;
    }

    @Override
    public String toString() {
        return oldState + " -> " + currentState + " -> " + newState;
    }

    public boolean isAllFalse() {
        return !oldState && !currentState && !newState;
    }

    public boolean isNotAllFalse() {
        return oldState || currentState || newState;
    }

    public PlusMinusZero getRelativeRelativityMode() {
        return getRelativityMode(currentState, newState);
    }

    public PlusMinusZero getAbsoluteRelativityMode() {
        return getRelativityMode(oldState, newState);
    }

    private static PlusMinusZero getRelativityMode(boolean oldState, boolean newState) {
        if (oldState) {
            if (newState) {
                return PlusMinusZero.ZERO;
            } else {
                return PlusMinusZero.MINUS;
            }
        } else {
            if (newState) {
                return PlusMinusZero.PLUS;
            } else {
                return null;
            }
        }
    }
}
