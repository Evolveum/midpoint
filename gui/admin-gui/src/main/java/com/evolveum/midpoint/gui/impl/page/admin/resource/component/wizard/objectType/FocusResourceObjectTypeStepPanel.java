/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-type-focus",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.objectType.focus", icon = "fa fa-circle"),
        expanded = true)
public class FocusResourceObjectTypeStepPanel extends AbstractValueFormResourceWizardStepPanel<ResourceObjectFocusSpecificationType> {

    public static final String PANEL_TYPE = "rw-type-focus";

    private final IModel<PrismContainerValueWrapper<ResourceObjectFocusSpecificationType>> valueModel;

    public FocusResourceObjectTypeStepPanel(ResourceDetailsModel model,
                                            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, null);
        this.valueModel = createNewValueModel(newValueModel, ResourceObjectTypeDefinitionType.F_FOCUS);
    }

    @Override
    public IModel<PrismContainerValueWrapper<ResourceObjectFocusSpecificationType>> getValueModel() {
        return valueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.focus");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.focus.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.focus.subText");
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return createStringResource("PageResource.wizard.step.objectType.focus.subText");
    }
}
