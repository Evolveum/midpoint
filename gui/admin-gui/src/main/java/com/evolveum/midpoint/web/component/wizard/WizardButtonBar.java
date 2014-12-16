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

package com.evolveum.midpoint.web.component.wizard;

import org.apache.wicket.extensions.wizard.*;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * @author lazyman
 */
public class WizardButtonBar extends Panel implements IDefaultButtonProvider {

    public WizardButtonBar(String id, IWizard wizard) {
        super(id);
        add(new PreviousButton("previous", wizard));
        add(new NextButton("next", wizard));
        add(new LastButton("last", wizard));
        add(new CancelButton("cancel", wizard));
        add(new FinishButton("finish", wizard){

            /*
            *   Finish button is always enabled, so user don't have to
            *   click through every step of wizard every time it is used
            * */
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
    }

    @Override
    public IFormSubmittingComponent getDefaultButton(IWizardModel model) {

        if (model.isNextAvailable()){
            return (IFormSubmittingComponent)get("next");
        }

        else if (model.isLastAvailable()){
            return (IFormSubmittingComponent)get("last");
        }

        else if (model.isLastStep(model.getActiveStep())){
            return (IFormSubmittingComponent)get("finish");
        }

        return null;
    }
}
