/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.WizardListener;
import com.evolveum.midpoint.gui.impl.component.message.Callout;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class NotFoundWizardVariableStepPanel
        extends AbstractWizardStepPanel implements WizardListener {

    private static final String ID_CALLOUT = "callout";

    public NotFoundWizardVariableStepPanel() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(Callout.createInfoCallout(
                ID_CALLOUT,
                createStringResource("NotFoundWizardVariableStepPanel.message.inapplicablePanel")));
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("tempMessagePanel.message.undefined");
    }

    @Override
    protected IModel<?> getTextModel() {
        return getTitle();
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return Model.of();
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-10 col-sm-12";
    }
}
