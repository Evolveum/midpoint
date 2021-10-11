/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.delta.AddDeleteReplace;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes assignment origin e.g. if it's in object old, current, or in delta; if it's virtual or not.
 */
public class AssignmentOrigin {

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
     * Assignment is present in assignment-related delta (either as ADD, DELETE or REPLACE value).
     */
    private boolean isChanged;

    /**
     * For isChanged: is the assignment in DELTA ADD?
     */
    private boolean isInDeltaAdd;

    /**
     * For isChanged: is the assignment in DELTA DELETE?
     */
    private boolean isInDeltaDelete;

    /**
     * For isChanged: is the assignment in DELTA REPLACE?
     */
    private boolean isInDeltaReplace;

    AssignmentOrigin(boolean virtual) {
        this.virtual = virtual;
    }

    public static AssignmentOrigin createInObject() {
        AssignmentOrigin rv = new AssignmentOrigin(false);
        rv.isCurrent = true;
        return rv;
    }

    public static AssignmentOrigin createNotVirtual() {
        return new AssignmentOrigin(false);
    }

    public static AssignmentOrigin createVirtual() {
        return new AssignmentOrigin(true);
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public boolean isOld() {
        return isOld;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public boolean isInDeltaAdd() {
        return isInDeltaAdd;
    }

    public boolean isInDeltaDelete() {
        return isInDeltaDelete;
    }

    public boolean isInDeltaReplace() {
        return isInDeltaReplace;
    }

    @Override
    public String toString() {
        List<String> labels = new ArrayList<>();
        addLabel(labels, isCurrent,"current");
        addLabel(labels, isOld,"old");
        addLabel(labels, isChanged,"changed");
        addLabel(labels, isInDeltaAdd, "inDeltaAdd");
        addLabel(labels, isInDeltaDelete, "inDeltaDelete");
        addLabel(labels, isInDeltaReplace, "inDeltaReplace");
        return String.join(", ", labels.toArray(new String[0]));
    }

    private void addLabel(List<String> labels, boolean flagValue, String label) {
        if (flagValue) {
            labels.add(label);
        }
    }

    void updateFlags(SmartAssignmentCollection.Mode mode, AddDeleteReplace deltaSet) {
        switch (mode) {
            case CURRENT:
                isCurrent = true;
                break;
            case OLD:
                isOld = true;
                break;
            case CHANGED:
                isChanged = true;
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
                isInDeltaReplace = true;
                break;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Assignment is either being added in the current wave or was added in some of the previous waves.
     * EXPERIMENTAL. USE WITH CARE.
     */
    public boolean isBeingAdded() {
        return !isOld && (isInDeltaAdd || isInDeltaReplace || isCurrent);
    }

    /**
     * Assignment is either being deleted in the current wave or was deleted in some of the previous waves.
     * EXPERIMENTAL. USE WITH CARE.
     */
    public boolean isBeingDeleted() {
        return isOld && (isInDeltaDelete || !isCurrent);        // TODO what about replace deltas?
    }

    /**
     * Assignment was present at the beginning and is not being deleted.
     * EXPERIMENTAL. USE WITH CARE.
     */
    public boolean isBeingModified() {
        return isOld && !isBeingAdded() && !isBeingDeleted();
    }
}
