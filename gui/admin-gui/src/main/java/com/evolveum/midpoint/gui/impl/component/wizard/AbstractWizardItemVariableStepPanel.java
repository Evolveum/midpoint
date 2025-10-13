/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serializable;

public abstract class AbstractWizardItemVariableStepPanel implements Serializable {

    private AbstractWizardStepPanel<?> panel;

    public abstract boolean isApplicable();

    public abstract AbstractWizardStepPanel<?> createStepWizardPanel();

    public final AbstractWizardStepPanel<?> getStepWizardPanel() {
        if (panel == null) {
            panel = createStepWizardPanel();
        }
        return panel;
    };
}
