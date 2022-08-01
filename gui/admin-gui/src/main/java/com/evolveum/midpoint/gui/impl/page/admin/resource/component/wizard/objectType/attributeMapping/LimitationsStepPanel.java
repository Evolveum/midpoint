/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.AbstractFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "LimitationsMappingWizard")
@PanelInstance(identifier = "LimitationsMappingWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.attributes.step.limitation", icon = "fa fa-circle"),
        containerPath = "schemaHandling/objectType/attribute/limitations",
        expanded = true)
public class LimitationsStepPanel extends AbstractFormResourceWizardStepPanel {

    private static final String PANEL_TYPE = "LimitationsMappingWizard";

    private final IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> newValueModel;

    public LimitationsStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> newValueModel) {
        super(model);
        this.newValueModel = newValueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        PrismContainerWrapperModel<ResourceAttributeDefinitionType, Containerable> model
                = PrismContainerWrapperModel.fromContainerValueWrapper(newValueModel, ResourceAttributeDefinitionType.F_LIMITATIONS);
        model.getObject().setExpanded(true);
        return model;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    @Override
    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.attributes.step.limitation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.attributes.limitation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.attributes.limitation.subText");
    }
}
