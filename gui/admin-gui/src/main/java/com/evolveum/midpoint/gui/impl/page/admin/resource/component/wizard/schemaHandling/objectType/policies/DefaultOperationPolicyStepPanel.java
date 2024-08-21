/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-type-default-operation-policy",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.objectType.defaultOperationPolicy", icon = "fa fa-circle"))
public class DefaultOperationPolicyStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-type-default-operation-policy";

    public DefaultOperationPolicyStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, newValueModel, null);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.defaultOperationPolicy");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.defaultOperationPolicy.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.defaultOperationPolicy.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ResourceObjectTypeDefinitionType.F_DEFAULT_OPERATION_POLICY_REF)) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
