/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardStepPanel<T> extends BasePanel<T> implements WizardStep {

    private WizardModel wizard;

    public WizardStepPanel() {
        super(WizardPanel.ID_CONTENT_BODY);
    }

    public WizardStepPanel(IModel<T> model) {
        super(WizardPanel.ID_CONTENT_BODY, model);
    }

    @Override
    public void init(WizardModel wizard) {
        this.wizard = wizard;
    }

    public WizardModel getWizard() {
        return wizard;
    }
}
