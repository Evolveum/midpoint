/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.AbstractFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "defaultSettingSynchronizationWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.synchronization.defaultSettings", icon = "fa fa-circle"),
        containerPath = "schemaHandling/objectType/synchronization/defaultSettings",
        expanded = true)
public class DefaultSettingStepPanel extends AbstractFormResourceWizardStepPanel {

    private static final String PANEL_TYPE = "defaultSettingSynchronizationWizard";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel;

    public DefaultSettingStepPanel(ResourceDetailsModel model,
                                   IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newValueModel) {
        super(model);
        this.newValueModel = newValueModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        PrismContainerWrapperModel<ResourceObjectTypeDefinitionType, Containerable> model
                = PrismContainerWrapperModel.fromContainerValueWrapper(
                        newValueModel,
                        ItemPath.create(
                                ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION,
                                SynchronizationReactionsType.F_DEFAULT_SETTINGS));
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
        return createStringResource("PageResource.wizard.step.synchronization.defaultSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.defaultSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.defaultSettings.subText");
    }
}
