/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard;

import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.PreviousButton;

/**
 * @author mederly
 */
public class ResourceWizardPreviousButton extends PreviousButton {

    public ResourceWizardPreviousButton(String id, IWizard wizard) {
        super(id, wizard);
        setDefaultFormProcessing(true);
    }

    @Override
    public void onClick() {
        IWizardModel wizardModel = getWizardModel();
        IWizardStep step = wizardModel.getActiveStep();
        step.applyState();
        super.onClick();
    }
}
