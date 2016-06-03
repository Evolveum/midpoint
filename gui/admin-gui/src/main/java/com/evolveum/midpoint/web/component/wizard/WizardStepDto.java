/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
