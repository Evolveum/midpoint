/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityScriptPanel extends BasePanel<CapabilityDto<ScriptCapabilityType>> {

    private static final String ID_ENABLED = "enabled";
    private static final String ID_ON_CONNECTOR = "onConnectorValue";
    private static final String ID_ON_RESOURCE = "onResourceValue";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_ON_CONNECTOR = "onConnectorTooltip";
    private static final String ID_T_ON_RESOURCE = "onResourceTooltip";

    public CapabilityScriptPanel(String componentId, IModel<CapabilityDto<ScriptCapabilityType>> model, WebMarkupContainer capabilitiesTable,
            PageResourceWizard parentPage) {
        super(componentId, model);
        initLayout(capabilitiesTable, parentPage);
    }

    protected void initLayout(final WebMarkupContainer capabilitiesTable, PageResourceWizard parentPage) {
        parentPage.addEditingEnabledBehavior(this);

        CheckBox enabled = new CheckBox(ID_ENABLED, new PropertyModel<>(getModel(), "capability.enabled"));
        enabled.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(capabilitiesTable);
            }
        });
        add(enabled);

        MultiValueTextPanel<?> onConnector = new MultiValueTextPanel(
                ID_ON_CONNECTOR, prepareOnConnectorModel(), parentPage.getReadOnlyModel(), true);
        add(onConnector);

        MultiValueTextPanel<?> onResource = new MultiValueTextPanel(
                ID_ON_RESOURCE, prepareOnResourceModel(), parentPage.getReadOnlyModel(), true);
        add(onResource);

        Label enabledTooltip = new Label(ID_T_ENABLED);
        enabledTooltip.add(new InfoTooltipBehavior());
        add(enabledTooltip);

        Label onConnectorTooltip = new Label(ID_T_ON_CONNECTOR);
        onConnectorTooltip.add(new InfoTooltipBehavior());
        add(onConnectorTooltip);

        Label onResourceTooltip = new Label(ID_T_ON_RESOURCE);
        onResourceTooltip.add(new InfoTooltipBehavior());
        add(onResourceTooltip);
    }

    private IModel<?> prepareOnConnectorModel() {
        CapabilityDto<?> dto = getModel().getObject();
        ScriptCapabilityType script = (ScriptCapabilityType) dto.getCapability();

        for (ScriptCapabilityType.Host host : script.getHost()) {
            if (ProvisioningScriptHostType.CONNECTOR.equals(host.getType())) {
                return new PropertyModel<List<String>>(host, "language");
            }
        }

        List<String> emptyList = new ArrayList<>();
        return Model.of(emptyList);
    }

    private IModel<?> prepareOnResourceModel() {
        CapabilityDto<?> dto = getModel().getObject();
        ScriptCapabilityType script = (ScriptCapabilityType) dto.getCapability();

        for (ScriptCapabilityType.Host host : script.getHost()) {
            if (ProvisioningScriptHostType.RESOURCE.equals(host.getType())) {
                return new PropertyModel<List<String>>(host, "language");
            }
        }

        List<String> emptyList = new ArrayList<>();
        return Model.of(emptyList);
    }
}
