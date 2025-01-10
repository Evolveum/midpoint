/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundMappingMainConfigurationStepPanel;

import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-activation-inbound-main",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.activation.inbound.main", icon = "fa fa-screwdriver-wrench"),
        expanded = true)
public class InboundActivationMappingMainConfigurationStepPanel
        extends InboundMappingMainConfigurationStepPanel {

    public static final String PANEL_TYPE = "rw-activation-inbound-main";

    public InboundActivationMappingMainConfigurationStepPanel(ResourceDetailsModel model,
                                                    IModel<PrismContainerValueWrapper<MappingType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return
                createStringResource("PageResource.wizard.step.activation.inbound.main");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.activation.inbound.main.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource(
                "PageResource.wizard.step.activation.inbound.main.subText",
                GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getNewValue()));
    }

    @Override
    protected LoadableDetachableModel<String> createLabelModel() {
        return (LoadableDetachableModel<String>) getTextModel();
    }
}
