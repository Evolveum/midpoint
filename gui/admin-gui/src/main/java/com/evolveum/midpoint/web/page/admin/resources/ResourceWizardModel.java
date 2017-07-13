/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Because ResourceWizard is so simple, we want to change Previous button semantics:
 * it should go to physically previous step (in the list of steps); not to the previously visited step.
 *
 * Currently we ignore that history list grows bigger on each 'next' click.
 *
 * This class provides the custom previous/isPreviousAvailable functionality.
 *
 * @author mederly
 */
public class ResourceWizardModel extends WizardModel {

	@NotNull final private PageResourceWizard parentPage;

	public ResourceWizardModel(PageResourceWizard parentPage) {
		this.parentPage = parentPage;
	}

	@Override
	public void previous() {
		Integer i = getCurrentStepIndex();
		if (i == null || i == 0) {
			return;		// at the beginning OR some weird situation - nothing to do
		}
		setActiveStep(getStep(i-1));
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
