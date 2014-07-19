/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class CapabilityCredentialsPanel extends SimplePanel{

    private static final String ID_ENABLED = "enabled";
    private static final String ID_LABEL_PASSWORD = "password";
    private static final String ID_PASSWORD_ENABLED = "passEnabled";
    private static final String ID_PASSWORD_RETURNED = "passReturned";

    public CapabilityCredentialsPanel(String componentId, IModel<CapabilityDto> model){
        super(componentId, model);
    }

    @Override
    protected void initLayout(){
        Label passLabel = new Label(ID_LABEL_PASSWORD, createStringResource("capabilityCredentialsPanel.label.password"));
        add(passLabel);

        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<Boolean>(getModel(), "capability.enabled"));
        add(enabled);

        CheckBox passwordEnabled = new CheckBox(ID_PASSWORD_ENABLED, new PropertyModel<Boolean>(getModel(), "capability.password.enabled"));
        add(passwordEnabled);

        CheckBox passwordReturned = new CheckBox(ID_PASSWORD_RETURNED, new PropertyModel<Boolean>(getModel(), "capability.password.returnedByDefault"));
        add(passwordReturned);
    }
}
