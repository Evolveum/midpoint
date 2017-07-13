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
