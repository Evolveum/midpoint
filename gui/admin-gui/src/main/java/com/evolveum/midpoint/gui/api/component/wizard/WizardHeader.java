/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardHeader extends NavigationPanel implements WizardListener {

    private static final long serialVersionUID = 1L;

    private WizardModel model;

    public WizardHeader(String id, WizardModel model) {
        super(id);

        this.model = model;

        model.addWizardListener(this);
    }

    @Override
    protected IModel<String> createTitleModel() {
        return () -> model.getActiveStep().getTitle().getObject();
    }

    @Override
    protected IModel<String> createNextTitleModel() {
        return () -> {
            WizardStep next = model.getNextPanel();
            return next != null ? next.getTitle().getObject() : null;
        };
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        addOrReplace(createHeaderContent());
    }
}
