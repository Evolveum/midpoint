/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-attribute",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.mainConfiguration", icon = "fa fa-circle"),
        expanded = true)
public class MainConfigurationStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ResourceAttributeDefinitionType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-attribute";

    public MainConfigurationStepPanel(ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.attributes.mainConfiguration");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.mainConfiguration.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.mainConfiguration.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ResourceAttributeDefinitionType.F_LIFECYCLE_STATE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }
}
