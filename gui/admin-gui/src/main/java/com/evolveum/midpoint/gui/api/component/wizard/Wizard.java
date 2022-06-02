/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Wizard implements Serializable {

    private static final long serialVersionUID = 1L;

    private int stepCount;
    private int activeStepIndex;

    public Wizard(int stepCount) {
        this.stepCount = stepCount;
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
    }

    public void setActiveStepIndex(int activeStepIndex) {
        if (activeStepIndex < 0) {
            activeStepIndex = 0;
        }
        if (activeStepIndex >= stepCount) {
            activeStepIndex = stepCount - 1;
        }

        this.activeStepIndex = activeStepIndex;
    }

    public void nextStep() {
        if (activeStepIndex + 1 >= stepCount) {
            return;
        }

        activeStepIndex++;
    }

    public void previousStep() {
        if (activeStepIndex <= 0) {
            return;
        }
        activeStepIndex--;
    }
}
