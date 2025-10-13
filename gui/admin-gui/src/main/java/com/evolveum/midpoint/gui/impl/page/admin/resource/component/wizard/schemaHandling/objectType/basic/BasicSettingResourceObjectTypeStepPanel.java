/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-type-basic",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.objectType.basicSettings", icon = "fa fa-circle"))
public class BasicSettingResourceObjectTypeStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-type-basic";

    public BasicSettingResourceObjectTypeStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model, newValueModel, null);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.basicSettings.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ResourceObjectTypeDefinitionType.F_KIND)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }
}
