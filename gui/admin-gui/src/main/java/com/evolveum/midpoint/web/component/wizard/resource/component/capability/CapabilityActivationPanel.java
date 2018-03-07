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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
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
public class CapabilityActivationPanel extends BasePanel {

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
    private static final String ID_CHECK_LOCKOUT_ENABLED = "lockoutEnabled";
    private static final String ID_CHECK_LOCKOUT_RETURNED = "lockoutReturnedByDefault";
    private static final String ID_CHECK_LOCKOUT_IGNORE = "lockoutIgnoreAttribute";
    private static final String ID_LOCKOUT_NORMAL_LIST = "lockoutNormalList";
    private static final String ID_LOCKOUT_LOCKED_LIST = "lockoutLockedList";
    private static final String ID_SELECT_LOCKOUT = "lockoutSelect";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_RETURNED_BY_DEFAULT = "returnedByDefaultTooltip";
    private static final String ID_T_IGNORE_ATTR = "ignoreAttributeTooltip";
    private static final String ID_T_ATTR_NAME = "attributeNameTooltip";
    private static final String ID_T_ENABLE_LIST = "enableListTooltip";
    private static final String ID_T_DISABLE_LIST = "disableListTooltip";
    private static final String ID_T_L_ENABLED = "lockoutEnabledTooltip";
    private static final String ID_T_L_RETURNED_BY_DEFAULT = "lockoutReturnedByDefaultTooltip";
    private static final String ID_T_L_IGNORE_ATTR = "lockoutIgnoreAttributeTooltip";
    private static final String ID_T_L_ATTR_NAME = "lockoutAttributeNameTooltip";
    private static final String ID_T_L_NORMAL_LIST = "lockoutNormalListTooltip";
    private static final String ID_T_L_LOCKED_LIST = "lockoutLockedListTooltip";
    private static final String ID_T_V_FROM_ENABLED = "validFromEnabledTooltip";
    private static final String ID_T_V_FROM_RETURN = "validFromReturnedTooltip";
    private static final String ID_T_V_TO_ENABLED = "validToEnabledTooltip";
    private static final String ID_T_V_TO_RETURN = "validToReturnedTooltip";

    public CapabilityActivationPanel(String componentId, IModel<CapabilityDto<ActivationCapabilityType>> model, PageResourceWizard parentPage) {
        super(componentId, model);
		initLayout(parentPage);
    }

    protected void initLayout(PageResourceWizard parentPage) {
		parentPage.addEditingEnabledBehavior(this);

        CheckBox validFromEnabled = new CheckBox(ID_CHECK_VALID_FROM_ENABLED,
            new PropertyModel<>(getModel(), "capability.validFrom.enabled"));
        add(validFromEnabled);

        CheckBox validFromReturned = new CheckBox(ID_CHECK_VALID_FROM_RETURNED,
            new PropertyModel<>(getModel(), "capability.validFrom.returnedByDefault"));
        add(validFromReturned);

        CheckBox validToEnabled = new CheckBox(ID_CHECK_VALID_TO_ENABLED,
            new PropertyModel<>(getModel(), "capability.validTo.enabled"));
        add(validToEnabled);

        CheckBox validToReturned = new CheckBox(ID_CHECK_VALID_TO_RETURNED,
            new PropertyModel<>(getModel(), "capability.validTo.returnedByDefault"));
        add(validToReturned);

        add(new CheckBox(ID_CHECK_STATUS_ENABLED, new PropertyModel<>(getModel(), "capability.status.enabled")));
		add(new CheckBox(ID_CHECK_STATUS_RETURNED, new PropertyModel<>(getModel(), "capability.status.returnedByDefault")));
        add(new CheckBox(ID_CHECK_STATUS_IGNORE, new PropertyModel<>(getModel(), "capability.status.ignoreAttribute")));

        MultiValueTextPanel statusEnableList = new MultiValueTextPanel<String>(ID_STATUS_ENABLE_LIST,
            new PropertyModel<>(getModel(), "capability.status.enableValue"), parentPage.getReadOnlyModel(), false) {

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(statusEnableList);

        MultiValueTextPanel statusDisableList = new MultiValueTextPanel<String>(ID_STATUS_DISABLE_LIST,
            new PropertyModel<>(getModel(), "capability.status.disableValue"), parentPage.getReadOnlyModel(), false) {

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(statusDisableList);

        IChoiceRenderer<QName> renderer = new QNameChoiceRenderer(true);

        DropDownChoice statusChoice = new DropDownChoice<>(ID_SELECT_STATUS,
            new PropertyModel<>(getModel(), "capability.status.attribute"),
                createAttributeChoiceModel(renderer), renderer);
        add(statusChoice);

        add(new CheckBox(ID_CHECK_LOCKOUT_ENABLED, new PropertyModel<>(getModel(), "capability.lockoutStatus.enabled")));
		add(new CheckBox(ID_CHECK_LOCKOUT_RETURNED, new PropertyModel<>(getModel(), "capability.lockoutStatus.returnedByDefault")));
        add(new CheckBox(ID_CHECK_LOCKOUT_IGNORE, new PropertyModel<>(getModel(), "capability.lockoutStatus.ignoreAttribute")));

        MultiValueTextPanel lockoutNormalList = new MultiValueTextPanel<String>(ID_LOCKOUT_NORMAL_LIST,
            new PropertyModel<>(getModel(), "capability.lockoutStatus.normalValue"), parentPage.getReadOnlyModel(), false) {

            @Override
            protected StringResourceModel createEmptyItemPlaceholder() {
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(lockoutNormalList);

        MultiValueTextPanel lockoutLockedList = new MultiValueTextPanel<String>(ID_LOCKOUT_LOCKED_LIST,
            new PropertyModel<>(getModel(), "capability.lockoutStatus.lockedValue"), parentPage.getReadOnlyModel(), false) {

            @Override
            protected StringResourceModel createEmptyItemPlaceholder(){
                return createStringResource("capabilityActivationPanel.list.placeholder");
            }
        };
        add(lockoutLockedList);

        IChoiceRenderer<QName> lockoutRenderer = new QNameChoiceRenderer(true);

        DropDownChoice lockoutChoice = new DropDownChoice<>(ID_SELECT_LOCKOUT,
            new PropertyModel<>(getModel(), "capability.lockoutStatus.attribute"),
                createAttributeChoiceModel(lockoutRenderer), lockoutRenderer);
        add(lockoutChoice);

		add(WebComponentUtil.createHelp(ID_T_L_ENABLED));
		add(WebComponentUtil.createHelp(ID_T_L_RETURNED_BY_DEFAULT));
		add(WebComponentUtil.createHelp(ID_T_L_IGNORE_ATTR));
		add(WebComponentUtil.createHelp(ID_T_L_ATTR_NAME));
		add(WebComponentUtil.createHelp(ID_T_L_NORMAL_LIST));
		add(WebComponentUtil.createHelp(ID_T_L_LOCKED_LIST));

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

    public IModel<List<QName>> createAttributeChoiceModel(IChoiceRenderer<QName> renderer){
        return null;
    }
}
