/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper;

import org.apache.commons.lang3.BooleanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Navigation model for {@link PopupStepperPanel}.
 *
 * <p>Maintains the ordered collection of {@link PopupStep steps},
 * tracks the currently active step, and provides navigation between
 * visible steps.</p>
 *
 * <p>Steps whose {@link PopupStep#isStepVisible()} evaluates to
 * {@code false} are automatically skipped during navigation and are
 * not included in step numbering.</p>
 */
public class PopupStepperModel implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final List<PopupStep> steps;
    private int activeStepIndex;

    public PopupStepperModel(List<PopupStep> steps) {
        this.steps = steps;
    }

    public void init() {
        steps.forEach(step -> step.init(this));

        for (int i = 0; i < steps.size(); i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                activeStepIndex = i;
                return;
            }
        }
    }

    public List<PopupStep> getSteps() {
        return steps;
    }

    public PopupStep getActiveStep() {
        return steps.get(activeStepIndex);
    }

    public boolean hasNext() {
        return findNextIndex() != -1;
    }

    public void next() {
        int next = findNextIndex();
        if (next != -1) {
            activeStepIndex = next;
        }
    }

    public boolean hasPrevious() {
        return findPreviousIndex() != -1;
    }

    public void previous() {
        int previous = findPreviousIndex();
        if (previous != -1) {
            activeStepIndex = previous;
        }
    }

    private int findNextIndex() {
        for (int i = activeStepIndex + 1; i < steps.size(); i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                return i;
            }
        }
        return -1;
    }

    private int findPreviousIndex() {
        for (int i = activeStepIndex - 1; i >= 0; i--) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                return i;
            }
        }
        return -1;
    }

    public int getActiveStepNumber() {
        int number = 0;

        for (int i = 0; i <= activeStepIndex; i++) {
            if (BooleanUtils.isTrue(steps.get(i).isStepVisible().getObject())) {
                number++;
            }
        }

        return number;
    }

    public int getVisibleStepCount() {
        int count = 0;

        for (PopupStep step : steps) {
            if (BooleanUtils.isTrue(step.isStepVisible().getObject())) {
                count++;
            }
        }

        return count;
    }
}
