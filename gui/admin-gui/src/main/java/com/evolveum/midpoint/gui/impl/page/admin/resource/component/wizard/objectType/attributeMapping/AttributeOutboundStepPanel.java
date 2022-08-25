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
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.AbstractFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractOutboundStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "attributeOutboundWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound", icon = "fa fa-circle"),
//        containerPath = "schemaHandling/objectType/attribute/outbound",
        expanded = true)
public class AttributeOutboundStepPanel extends AbstractValueFormResourceWizardStepPanel<MappingType> {

    private static final String PANEL_TYPE = "attributeOutboundWizard";

    public AttributeOutboundStepPanel(ResourceDetailsModel model,
                                             IModel<PrismContainerValueWrapper<MappingType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.attributes.step.outbound");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.attributes.outbound.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.attributes.outbound.subText");
    }
}
