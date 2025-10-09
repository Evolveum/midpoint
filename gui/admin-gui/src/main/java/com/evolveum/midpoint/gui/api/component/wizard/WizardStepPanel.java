/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
