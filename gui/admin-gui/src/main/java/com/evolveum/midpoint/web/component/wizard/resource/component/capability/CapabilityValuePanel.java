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
import com.evolveum.midpoint.web.component.wizard.resource.dto.Capability;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *  @author shood
 * */
public class CapabilityValuePanel extends BasePanel<CapabilityDto<CapabilityType>> {

    private static final String ID_LABEL = "label";
    private static final String ID_ENABLED = "enabled";

	private final WebMarkupContainer capabilityTable;

    public CapabilityValuePanel(String componentId, IModel<CapabilityDto<CapabilityType>> model, WebMarkupContainer capabilityTable,
			PageResourceWizard parentPage) {
        super(componentId, model);
		this.capabilityTable = capabilityTable;
		initLayout(parentPage);
    }

    protected void initLayout(PageResourceWizard parentPage) {

		parentPage.addEditingEnabledBehavior(this);

        Label label = new Label(ID_LABEL, createStringResource(getCapabilityLabelKey()));
        add(label);

        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<>(getModel(), "capability.enabled"));
		enabled.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(capabilityTable);
			}
		});
        add(enabled);
    }

    private String getCapabilityLabelKey(){
        CapabilityType capability = getModelObject().getCapability();
		return "capabilityValuePanel.label.capability." + Capability.getResourceKeyForClass(capability.getClass());
    }
}
