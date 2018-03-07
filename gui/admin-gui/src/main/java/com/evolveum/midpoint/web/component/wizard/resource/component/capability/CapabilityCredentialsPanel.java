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
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class CapabilityCredentialsPanel extends BasePanel<CapabilityDto<CredentialsCapabilityType>> {

    private static final String ID_ENABLED = "enabled";
    private static final String ID_LABEL_PASSWORD = "password";
    private static final String ID_PASSWORD_ENABLED = "passEnabled";
    private static final String ID_PASSWORD_RETURNED = "passReturned";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_PASS_ENABLED = "passEnabledTooltip";
    private static final String ID_T_PASS_RETURN = "passReturnedTooltip";

    public CapabilityCredentialsPanel(String componentId, IModel<CapabilityDto<CredentialsCapabilityType>> model,
			WebMarkupContainer capabilitiesTable, PageResourceWizard parentPage) {
        super(componentId, model);
		initLayout(capabilitiesTable, parentPage);
    }

    protected void initLayout(final WebMarkupContainer capabilitiesTable, PageResourceWizard parentPage) {

		parentPage.addEditingEnabledBehavior(this);

        Label passLabel = new Label(ID_LABEL_PASSWORD, createStringResource("capabilityCredentialsPanel.label.password"));
        add(passLabel);

        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<>(getModel(), "capability.enabled"));
		enabled.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(capabilitiesTable);
			}
		});
        add(enabled);

        CheckBox passwordEnabled = new CheckBox(ID_PASSWORD_ENABLED, new PropertyModel<>(getModel(), "capability.password.enabled"));
        add(passwordEnabled);

        CheckBox passwordReturned = new CheckBox(ID_PASSWORD_RETURNED, new PropertyModel<>(getModel(), "capability.password.returnedByDefault"));
        add(passwordReturned);

        Label enabledTooltip = new Label(ID_T_ENABLED);
        enabledTooltip.add(new InfoTooltipBehavior());
        add(enabledTooltip);

        Label passEnabledTooltip = new Label(ID_T_PASS_ENABLED);
        passEnabledTooltip.add(new InfoTooltipBehavior());
        add(passEnabledTooltip);

        Label passReturnTooltip = new Label(ID_T_PASS_RETURN);
        passReturnTooltip.add(new InfoTooltipBehavior());
        add(passReturnTooltip);
    }
}
