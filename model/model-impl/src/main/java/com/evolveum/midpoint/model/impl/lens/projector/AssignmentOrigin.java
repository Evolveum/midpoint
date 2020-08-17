/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes assignment origin e.g. if it's in object old, current, or in delta; if it's virtual or not.
 */
public class AssignmentOrigin extends AbstractFreezable {

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

    public boolean isNew() {
        return Objects.requireNonNull(isNew, "Cannot ask isNew on unfrozen assignment collection");
    }

    public void setNew(boolean value) {
        checkMutable();
        isNew = value;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public boolean isOld() {
        return isOld;
    }

    public boolean isInDeltaAdd() {
        return isInDeltaAdd;
    }

    public boolean isInDeltaDelete() {
        return isInDeltaDelete;
    }

    @Override
    public String toString() {
        List<String> labels = new ArrayList<>();
        addLabel(labels, isOld,"old");
        addLabel(labels, isCurrent,"current");
        addLabel(labels, isNew,"new");
        addLabel(labels, isInDeltaAdd, "inDeltaAdd");
        addLabel(labels, isInDeltaDelete, "inDeltaDelete");
        return String.join(", ", labels.toArray(new String[0]));
    }

    private void addLabel(List<String> labels, Boolean flagValue, String label) {
        if (Boolean.TRUE.equals(flagValue)) {
            labels.add(label);
        }
    }

    void update(SmartAssignmentCollection.Mode mode, AddDeleteReplace deltaSet) {
        checkMutable();
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

    /**
     * Assignment is either being added in the current wave or was added in some of the previous waves.
     */
    public boolean isBeingAdded() {
        return !isOld && isNew();
    }

    /**
     * Assignment is either being deleted in the current wave or was deleted in some of the previous waves.
     */
    public boolean isBeingDeleted() {
        return isOld && !isNew();
    }

    /**
     * Assignment was present at the beginning and is not being deleted.
     */
    public boolean isBeingKept() {
        return isOld && isNew();
    }

    /**
     * Returns absolute mode of this assignment with regard to focus old state.
     */
    public PlusMinusZero getAbsoluteMode() {
        if (isBeingAdded()) {
            return PlusMinusZero.PLUS;
        } else if (isBeingDeleted()) {
            return PlusMinusZero.MINUS;
        } else {
            return PlusMinusZero.ZERO;
        }
    }

    @Override
    public void performFreeze() {
        if (isNew == null) {
            isNew = isInDeltaAdd || isCurrent && !isInDeltaDelete;
        }
    }
}
