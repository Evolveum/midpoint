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
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityActivationPanel  extends SimplePanel{

    private static final String ID_LABEL = "label";
    private static final String ID_LABEL_ENABLED_DISABLED = "enabledDisabledLabel";
    private static final String ID_CHECK_ENABLED = "enabled";
    private static final String ID_CHECK_RETURNED = "returnedByDefault";
    private static final String ID_CHECK_IGNORE = "ignoreAttribute";
    private static final String ID_ENABLE_LIST = "enableList";
    private static final String ID_DISABLE_LIST = "disableList";
    private static final String ID_LABEL_STATUS = "labelStatus";
    private static final String ID_LABEL_VALID_FROM = "labelValidFrom";
    private static final String ID_LABEL_VALID_TO = "labelValidTo";
    private static final String ID_CHECK_VALID_FROM_ENABLED = "validFromEnabled";
    private static final String ID_CHECK_VALID_FROM_RETURNED = "validFromReturned";
    private static final String ID_CHECK_VALID_TO_ENABLED = "validToEnabled";
    private static final String ID_CHECK_VALID_TO_RETURNED = "validToReturned";


    public CapabilityActivationPanel(String componentId, IModel<CapabilityDto> model){
        super(componentId, model);
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, createStringResource("capabilityActivationPanel.label"));
        add(label);

        Label enableDisableLabel = new Label(ID_LABEL_ENABLED_DISABLED,
                createStringResource("capabilityActivationPanel.label.enabledDisabled"));
        add(enableDisableLabel);

        Label statusLabel = new Label(ID_LABEL_STATUS,
                createStringResource("capabilityActivationPanel.label.status"));
        add(statusLabel);

        Label validFromLabel = new Label(ID_LABEL_VALID_FROM,
                createStringResource("capabilityActivationPanel.label.validFrom"));
        add(validFromLabel);

        Label validToLabel = new Label(ID_LABEL_VALID_TO,
                createStringResource("capabilityActivationPanel.label.validTo"));
        add(validToLabel);

        CheckBox enabled = new CheckBox(ID_CHECK_ENABLED,
                new PropertyModel<Boolean>(getModel(), "capability.enableDisable.enabled."));
        add(enabled);

        CheckBox returnedByDefault = new CheckBox(ID_CHECK_RETURNED,
                new PropertyModel<Boolean>(getModel(), "capability.enableDisable.returnedByDefault"));
        add(returnedByDefault);

        CheckBox ignoreAttribute = new CheckBox(ID_CHECK_IGNORE,
                new PropertyModel<Boolean>(getModel(), "capability.enableDisable.ignoreAttribute"));
        add(ignoreAttribute);

        CheckBox validFromEnabled = new CheckBox(ID_CHECK_VALID_FROM_ENABLED,
                new PropertyModel<Boolean>(getModel(), "capability.validFrom.enabled"));
        add(validFromEnabled);

        CheckBox validFromReturned = new CheckBox(ID_CHECK_VALID_FROM_RETURNED,
                new PropertyModel<Boolean>(getModel(), "capability.validFrom.returnedByDefault"));
        add(validFromReturned);

        CheckBox validToEnabled = new CheckBox(ID_CHECK_VALID_TO_ENABLED,
                new PropertyModel<Boolean>(getModel(), "capability.validTo.enabled"));
        add(validToEnabled);

        CheckBox validToReturned = new CheckBox(ID_CHECK_VALID_TO_RETURNED,
                new PropertyModel<Boolean>(getModel(), "capability.validTo.returnedByDefault"));
        add(validToReturned);

        CapabilityListRepeater enableList = new CapabilityListRepeater(ID_ENABLE_LIST,
                new PropertyModel<List<String>>(getModel(), "capability.enableDisable.enableValue")){

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(enableList);

        CapabilityListRepeater disableList = new CapabilityListRepeater(ID_DISABLE_LIST,
                new PropertyModel<List<String>>(getModel(), "capability.enableDisable.disableValue")){

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(disableList);
    }

}
