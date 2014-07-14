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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityScriptPanel extends SimplePanel {

    private static final String ID_ENABLED = "enabled";
    private static final String ID_ON_CONNECTOR = "onConnectorValue";
    private static final String ID_ON_RESOURCE = "onResourceValue";

    public CapabilityScriptPanel(String componentId, IModel<CapabilityDto> model){
        super(componentId, model);
    }

    @Override
    protected void initLayout(){
        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<Boolean>(getModel(), "capability.enabled"));
        add(enabled);

        CapabilityListRepeater onConnector = new CapabilityListRepeater(ID_ON_CONNECTOR, prepareOnConnectorModel());
        add(onConnector);

        CapabilityListRepeater onResource = new CapabilityListRepeater(ID_ON_RESOURCE, Model.of(prepareOnResourceModel()));
        add(onResource);
    }

    private IModel prepareOnConnectorModel(){
        CapabilityDto dto = (CapabilityDto)getModel().getObject();
        ScriptCapabilityType script = (ScriptCapabilityType)dto.getCapability();

        for(ScriptCapabilityType.Host host: script.getHost()){
            if(ProvisioningScriptHostType.CONNECTOR.equals(host.getType())){
                return new PropertyModel<List<String>>(host, "language");
            }
        }

        List<String> emptyList = new ArrayList<>();
        return Model.of(emptyList);
    }

    private IModel prepareOnResourceModel(){
        CapabilityDto dto = (CapabilityDto)getModel().getObject();
        ScriptCapabilityType script = (ScriptCapabilityType)dto.getCapability();

        for(ScriptCapabilityType.Host host: script.getHost()){
            if(ProvisioningScriptHostType.RESOURCE.equals(host.getType())){
                return new PropertyModel<List<String>>(host, "language");
            }
        }

        List<String> emptyList = new ArrayList<>();
        return Model.of(emptyList);
    }
}
