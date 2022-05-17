/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Wizard implements Serializable {

    private List<IModel<String>> stepLabels;

    private int activeStepIndex;

    public List<IModel<String>> getStepLabels() {
        if (stepLabels == null) {
            stepLabels = new ArrayList<>();
        }
        return stepLabels;
    }

    public void setStepLabels(List<IModel<String>> stepLabels) {
        this.stepLabels = stepLabels;
    }

    public int getActiveStepIndex() {
        return activeStepIndex;
    }

    public void setActiveStepIndex(int activeStepIndex) {
        this.activeStepIndex = activeStepIndex;
    }
}
