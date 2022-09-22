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
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "limitationsMappingWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.limitation", icon = "fa fa-circle"),
        expanded = true)
public class LimitationsStepPanel extends AbstractValueFormResourceWizardStepPanel<PropertyLimitationsType> {

    private static final String PANEL_TYPE = "limitationsMappingWizard";

    private final IModel<PrismContainerValueWrapper<PropertyLimitationsType>> valueModel;

    public LimitationsStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> parentModel) {
        super(model, null);
        this.valueModel = createNewValueModel(parentModel, ResourceAttributeDefinitionType.F_LIMITATIONS);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<PrismContainerValueWrapper<PropertyLimitationsType>> getValueModel() {
        return valueModel;
    }

    //    @Override
//    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
//        PrismContainerWrapperModel<ResourceAttributeDefinitionType, Containerable> model
//                = PrismContainerWrapperModel.fromContainerValueWrapper(newValueModel, ResourceAttributeDefinitionType.F_LIMITATIONS);
//        model.getObject().setExpanded(true);
//        return model;
//    }

    @Override
    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.attributes.limitation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.limitation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.limitation.subText");
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
