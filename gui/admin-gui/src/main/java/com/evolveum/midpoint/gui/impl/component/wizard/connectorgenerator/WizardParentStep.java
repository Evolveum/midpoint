/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;

import java.util.List;

public interface WizardParentStep extends WizardStep {

    default List<WizardStep> createChildrenSteps(){
        return List.of();
    }

    default String getDefaultStepId() {
        return getStepId();
    }

}
