/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Iterator;

import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardModel;

/**
 * Because ResourceWizard is so simple, we want to change Previous button semantics:
 * it should go to physically previous step (in the list of steps); not to the previously visited step.
 * <p>
 * Currently we ignore that history list grows bigger on each 'next' click.
 * <p>
 * This class provides the custom previous/isPreviousAvailable functionality.
 *
 * @author mederly
 */
public class ResourceWizardModel extends WizardModel {

    @Override
    public void previous() {
        Integer i = getCurrentStepIndex();
        if (i == null || i == 0) {
            return; // at the beginning OR some weird situation - nothing to do
        }
        setActiveStep(getStep(i - 1));
    }

    @Override
    public boolean isPreviousAvailable() {
        Integer i = getCurrentStepIndex();
        return i != null && i > 0 && getActiveStep().isComplete();
    }

    private Integer getCurrentStepIndex() {
        IWizardStep activeStep = getActiveStep();
        if (activeStep == null) {
            return null;
        }
        int index = 0;
        Iterator<IWizardStep> iterator = stepIterator();
        while (iterator.hasNext()) {
            if (activeStep.equals(iterator.next())) {
                return index;
            }
            index++;
        }
        return null;
    }

    private IWizardStep getStep(int index) {
        Iterator<IWizardStep> iterator = stepIterator();
        while (iterator.hasNext()) {
            IWizardStep currentStep = iterator.next();
            if (index == 0) {
                return currentStep;
            } else {
                index--;
            }
        }
        return null;
    }
}
