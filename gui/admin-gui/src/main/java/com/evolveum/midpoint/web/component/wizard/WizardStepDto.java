/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class WizardStepDto implements Serializable {

    private String name;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean active;
    private WizardStep wizardStep;

    public WizardStepDto(String name, WizardStep wizStep, boolean enabled, boolean visible) {
        this.name = name;
        this.enabled = enabled;
        this.visible = visible;
        this.wizardStep = wizStep;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public WizardStep getWizardStep() {
        return wizardStep;
    }
}
