/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

/**
 * Describes assignment origin, namely:
 *
 * - if it's in object old, current, or in delta;
 * - if it's virtual or not;
 * - where it originated - {@link ConfigurationItemOrigin}. TODO is this a good idea?
 *
 * Freezable, not immutable!
 */
public class AssignmentOrigin extends AbstractFreezable implements Serializable {

    /**
     * Assignment is virtual i.e. not really present in the focus object.
     *
     * It is derived e.g. from the task or forced from the lifecycle model.
     *
     * Virtual assignments are always with isCurrent = true.
     */
    private final boolean virtual;

    /**
     * For virtual assignments that came from activities.
     * This is the path of the activity where this virtual assignment is defined.
     *
     * An example: Policy rule "skip activity if too many deletions" is attached to an activity "X/Y" in the activity
     * tree. It may gets executed in Y or any of its children (like "X/Y/Z"). But the activityPath for such virtual
     * assignment is "X/Y" here, because it's defined for that activity.
     *
     * We assume that no assignment is both virtual and real, or virtual for multiple activities.
     * In such cases, results are unpredictable.
     */
    @Nullable private final ActivityPath activityPath;

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

    /** [EP:APSO] DONE */
    @NotNull private final ConfigurationItemOrigin configurationItemOrigin;

    AssignmentOrigin(
            boolean virtual,
            @Nullable ActivityPath activityPath,
            @NotNull ConfigurationItemOrigin configurationItemOrigin) {
        this.virtual = virtual;
        this.activityPath = activityPath;
        this.configurationItemOrigin = configurationItemOrigin; // [EP:APSO] DONE 4/4
    }

    public static AssignmentOrigin inObject(@NotNull ConfigurationItemOrigin configurationItemOrigin) {
        AssignmentOrigin rv = new AssignmentOrigin(false, null, configurationItemOrigin); // [EP:APSO] DONE 2/2
        rv.isCurrent = true;
        return rv;
    }

    /** Not in object, not virtual - to be used in tests. */
    @VisibleForTesting
    public static AssignmentOrigin other(@NotNull ConfigurationItemOrigin configurationItemOrigin) {
        return new AssignmentOrigin(false, null, configurationItemOrigin); // [EP:APSO] DONE (testing only)
    }

    public static AssignmentOrigin virtual(@Nullable ActivityPath activityPath, @NotNull ConfigurationItemOrigin configurationItemOrigin) {
        return new AssignmentOrigin(true, activityPath, configurationItemOrigin); // [EP:APSO] DONE 1/1
    }

    public boolean isVirtual() {
        return virtual;
    }

    public @Nullable ActivityPath getActivityPath() {
        return activityPath;
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

    public @NotNull ConfigurationItemOrigin getConfigurationItemOrigin() {
        return configurationItemOrigin; // [EP:APSO] DONE
    }

    @Override
    public String toString() {
        List<String> labels = new ArrayList<>();
        addLabel(labels, isOld,"old");
        addLabel(labels, isCurrent,"current");
        addLabel(labels, isNew,"new");
        addLabel(labels, isInDeltaAdd, "inDeltaAdd");
        addLabel(labels, isInDeltaDelete, "inDeltaDelete");
        labels.add("origin=" + configurationItemOrigin);
        if (activityPath != null) {
            labels.add("activity path: '" + activityPath + "'");
        }
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
            case CURRENT -> isCurrent = true;
            case OLD -> isOld = true;
            case NEW -> isNew = true;
            case IN_ADD_OR_DELETE_DELTA -> updateDeltaSetFlags(deltaSet);
            default -> throw new AssertionError();
        }
    }

    private void updateDeltaSetFlags(AddDeleteReplace deltaSet) {
        switch (deltaSet) {
            case ADD -> isInDeltaAdd = true;
            case DELETE -> isInDeltaDelete = true;
            case REPLACE -> throw new AssertionError("REPLACE values are treated in a special way");
            default -> throw new AssertionError();
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
