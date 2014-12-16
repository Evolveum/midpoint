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

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.web.component.wizard.WizardButtonBar;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.Wizard;

/**
 * @author lazyman
 */
public class ResourceWizard extends Wizard {

    public ResourceWizard(String id, IWizardModel wizardModel) {
        super(id, wizardModel);
    }

    @Override
    protected Component newButtonBar(String id) {
        return new WizardButtonBar(id, this);
    }
}
