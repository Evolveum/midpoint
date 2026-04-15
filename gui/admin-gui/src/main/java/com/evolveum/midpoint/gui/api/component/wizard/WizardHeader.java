/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardHeader extends NavigationPanel implements WizardListener {

    private static final long serialVersionUID = 1L;

    private WizardModelBasic model;

    public WizardHeader(String id, WizardModelBasic model) {
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
