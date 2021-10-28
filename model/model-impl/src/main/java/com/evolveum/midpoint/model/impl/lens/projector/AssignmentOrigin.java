/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.delta.AddDeleteReplace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes assignment origin e.g. if it's in object old, current, or in delta; if it's virtual or not.
 *
 * This is a stripped-down version back-ported from 4.4 to 4.0.x in order to fix MID-7317.
 * (By providing a serious way of knowing what assignments were really added and deleted during clockwork run.)
 */
public class AssignmentOrigin implements Serializable {

    /**
     * Assignment is virtual i.e. not really present in the focus object.
     *
     * It is derived e.g. from the task or forced from the lifecycle model.
     *
     * Virtual assignments are always with isCurrent = true.
     */
    private final boolean virtual;

    /**
     * Assignment is present in the current object.
     */
    private boolean isCurrent;

    /**
     * Assignment is present in the old object.
     */
    private boolean isOld;

    /**
     * Assignment is present in the new object.
     */
    private Boolean isNew;

    /**
     * For isChanged: is the assignment in DELTA ADD?
     */
    private boolean isInDeltaAdd;

    /**
     * For isChanged: is the assignment in DELTA DELETE?
     */
    private boolean isInDeltaDelete;

    /**
     * Is this assignment mentioned in the assignment delta? (either in add, delete, or replace set)?
     *
     * Special flag, just for implementing a mix-up of "old" (4.0) and "new" (4.2+) behavior.
     *
     * See MID-7317.
     */
    private boolean inDelta;

    AssignmentOrigin(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isNew() {
        return Objects.requireNonNull(isNew, "Cannot ask isNew on not-yet-computed assignment collection");
    }

    public void setNew(boolean value) {
        isNew = value;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public boolean isOld() {
        return isOld;
    }

    public boolean isInDelta() {
        return inDelta;
    }

    @Override
    public String toString() {
        List<String> labels = new ArrayList<>();
        addLabel(labels, isOld,"old");
        addLabel(labels, isCurrent,"current");
        addLabel(labels, isNew,"new");
        addLabel(labels, isInDeltaAdd, "inDeltaAdd");
        addLabel(labels, isInDeltaDelete, "inDeltaDelete");
        addLabel(labels, inDelta, "inDelta");
        return String.join(", ", labels.toArray(new String[0]));
    }

    private void addLabel(List<String> labels, Boolean flagValue, String label) {
        if (Boolean.TRUE.equals(flagValue)) {
            labels.add(label);
        }
    }

    void update(SmartAssignmentCollection.Mode mode, AddDeleteReplace deltaSet, boolean inDelta) {
        if (inDelta) {
            this.inDelta = true;
        }
        switch (mode) {
            case CURRENT:
                isCurrent = true;
                break;
            case OLD:
                isOld = true;
                break;
            case NEW:
                isNew = true;
                break;
            case IN_ADD_OR_DELETE_DELTA:
                updateDeltaSetFlags(deltaSet);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void updateDeltaSetFlags(AddDeleteReplace deltaSet) {
        switch (deltaSet) {
            case ADD:
                isInDeltaAdd = true;
                break;
            case DELETE:
                isInDeltaDelete = true;
                break;
            case REPLACE:
                throw new AssertionError("REPLACE values are treated in a special way");
            default:
                throw new AssertionError();
        }
    }

    void computeIsNew() {
        if (isNew == null) {
            isNew = isInDeltaAdd || isCurrent && !isInDeltaDelete;
        }
    }
}
