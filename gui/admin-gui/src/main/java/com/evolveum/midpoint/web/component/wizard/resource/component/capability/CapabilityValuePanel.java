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
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class CapabilityValuePanel extends SimplePanel{

    private static final String ID_LABEL = "label";
    private static final String ID_ENABLED = "enabled";

    public CapabilityValuePanel(String componentId, IModel<CapabilityDto> model){
        super(componentId, model);
    }

    @Override
    protected void initLayout(){

        Label label = new Label(ID_LABEL, createStringResource(getCapabilityLabelKey()));
        add(label);

        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<Boolean>(getModel(), "capability.enabled"));
        add(enabled);
    }

    private String getCapabilityLabelKey(){
        CapabilityType capability = ((CapabilityDto)getModel().getObject()).getCapability();

        if(capability instanceof ReadCapabilityType){
            return "capabilityValuePanel.label.capability.read";
        } else if(capability instanceof UpdateCapabilityType){
            return "capabilityValuePanel.label.capability.update";
        } else if(capability instanceof CreateCapabilityType){
            return "capabilityValuePanel.label.capability.create";
        } else if(capability instanceof DeleteCapabilityType){
            return "capabilityValuePanel.label.capability.delete";
        } else if(capability instanceof LiveSyncCapabilityType){
            return "capabilityValuePanel.label.capability.liveSync";
        } else if(capability instanceof TestConnectionCapabilityType){
            return "capabilityValuePanel.label.capability.testConnection";
        }
        return null;
    }
}
