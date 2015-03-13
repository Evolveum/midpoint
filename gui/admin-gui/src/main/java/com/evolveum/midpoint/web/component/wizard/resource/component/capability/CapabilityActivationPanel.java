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

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityActivationPanel extends SimplePanel{

    private static final String ID_CHECK_VALID_FROM_ENABLED = "validFromEnabled";
    private static final String ID_CHECK_VALID_FROM_RETURNED = "validFromReturned";
    private static final String ID_CHECK_VALID_TO_ENABLED = "validToEnabled";
    private static final String ID_CHECK_VALID_TO_RETURNED = "validToReturned";
    private static final String ID_CHECK_STATUS_ENABLED = "statusEnabled";
    private static final String ID_CHECK_STATUS_RETURNED = "statusReturnedByDefault";
    private static final String ID_CHECK_STATUS_IGNORE = "statusIgnoreAttribute";
    private static final String ID_STATUS_ENABLE_LIST = "statusEnableList";
    private static final String ID_STATUS_DISABLE_LIST = "statusDisableList";
    private static final String ID_SELECT_STATUS = "statusSelect";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_RETURNED_BY_DEFAULT = "returnedByDefaultTooltip";
    private static final String ID_T_IGNORE_ATTR = "ignoreAttributeTooltip";
    private static final String ID_T_ATTR_NAME = "attributeNameTooltip";
    private static final String ID_T_ENABLE_LIST = "enableListTooltip";
    private static final String ID_T_DISABLE_LIST = "disableListTooltip";
    private static final String ID_T_V_FROM_ENABLED = "validFromEnabledTooltip";
    private static final String ID_T_V_FROM_RETURN = "validFromReturnedTooltip";
    private static final String ID_T_V_TO_ENABLED = "validToEnabledTooltip";
    private static final String ID_T_V_TO_RETURN = "validToReturnedTooltip";

    public CapabilityActivationPanel(String componentId, IModel<CapabilityDto> model){
        super(componentId, model);
    }

    @Override
    protected void initLayout(){
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

        CheckBox statusEnabled = new CheckBox(ID_CHECK_STATUS_ENABLED,
                new PropertyModel<Boolean>(getModel(), "capability.status.enabled"));
        add(statusEnabled);

        CheckBox statusReturned = new CheckBox(ID_CHECK_STATUS_RETURNED,
                new PropertyModel<Boolean>(getModel(), "capability.status.returnedByDefault"));
        add(statusReturned);

        CheckBox statusIgnore = new CheckBox(ID_CHECK_STATUS_IGNORE,
                new PropertyModel<Boolean>(getModel(), "capability.status.ignoreAttribute"));
        add(statusIgnore);

        MultiValueTextPanel statusEnableList = new MultiValueTextPanel<String>(ID_STATUS_ENABLE_LIST,
                new PropertyModel<List<String>>(getModel(), "capability.status.enableValue")){

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(statusEnableList);

        MultiValueTextPanel statusDisableList = new MultiValueTextPanel<String>(ID_STATUS_DISABLE_LIST,
                new PropertyModel<List<String>>(getModel(), "capability.status.disableValue")){

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(statusDisableList);

        IChoiceRenderer<QName> renderer = new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                 return object.getLocalPart();
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        };

        DropDownChoice statusChoice = new DropDownChoice<>(ID_SELECT_STATUS,
                new PropertyModel<QName>(getModel(), "capability.status.attribute"),
                createAttributeChoiceModel(), renderer);
        add(statusChoice);

        Label enabledTooltip = new Label(ID_T_ENABLED);
        enabledTooltip.add(new InfoTooltipBehavior());
        add(enabledTooltip);

        Label returnTooltip = new Label(ID_T_RETURNED_BY_DEFAULT);
        returnTooltip.add(new InfoTooltipBehavior());
        add(returnTooltip);

        Label ignoreTooltip = new Label(ID_T_IGNORE_ATTR);
        ignoreTooltip.add(new InfoTooltipBehavior());
        add(ignoreTooltip);

        Label attributeNameTooltip = new Label(ID_T_ATTR_NAME);
        attributeNameTooltip.add(new InfoTooltipBehavior());
        add(attributeNameTooltip);

        Label enableListTooltip = new Label(ID_T_ENABLE_LIST);
        enableListTooltip.add(new InfoTooltipBehavior());
        add(enableListTooltip);

        Label disableListTooltip = new Label(ID_T_DISABLE_LIST);
        disableListTooltip.add(new InfoTooltipBehavior());
        add(disableListTooltip);

        Label vFromEnabledTooltip = new Label(ID_T_V_FROM_ENABLED);
        vFromEnabledTooltip.add(new InfoTooltipBehavior());
        add(vFromEnabledTooltip);

        Label vFromReturnTooltip = new Label(ID_T_V_FROM_RETURN);
        vFromReturnTooltip.add(new InfoTooltipBehavior());
        add(vFromReturnTooltip);

        Label vToEnabledTooltip = new Label(ID_T_V_TO_ENABLED);
        vToEnabledTooltip.add(new InfoTooltipBehavior());
        add(vToEnabledTooltip);

        Label vToReturnTooltip = new Label(ID_T_V_TO_RETURN);
        vToReturnTooltip.add(new InfoTooltipBehavior());
        add(vToReturnTooltip);
    }

    public IModel<List<QName>> createAttributeChoiceModel(){
        return null;
    }
}
