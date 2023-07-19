/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractSpecificMappingTileTable;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.AbstractSpecificMappingWizardPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */

@PanelType(name = "rw-activation")
@PanelInstance(identifier = "rw-activation",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.activation", icon = "fa fa-toggle-off"))
public abstract class ActivationMappingWizardPanel extends AbstractSpecificMappingWizardPanel<ResourceActivationDefinitionType> {

    public static final String PANEL_TYPE = "rw-activation";

    public ActivationMappingWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerWrapper<ResourceActivationDefinitionType>> containerModel,
            MappingDirection initialTab) {
        super(id, model, containerModel, initialTab);
    }

    @Override
    protected AbstractSpecificMappingTileTable<ResourceActivationDefinitionType> createTablePanel(String panelId, IModel<PrismContainerWrapper<ResourceActivationDefinitionType>> containerModel, MappingDirection mappingDirection) {
        return new ActivationMappingTileTable(panelId, containerModel, mappingDirection, getAssignmentHolderDetailsModel()) {
            @Override
            protected void editPredefinedMapping(
                    IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
                    AjaxRequestTarget target) {
                ActivationMappingWizardPanel.this.editPredefinedMapping(valueModel, mappingDirection, target);
            }

            @Override
            protected void editConfiguredMapping(
                    IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                ActivationMappingWizardPanel.this.editConfiguredMapping(valueModel, mappingDirection, target);
            }

            @Override
            public void refresh(AjaxRequestTarget target) {
                super.refresh(target);
                target.add(getParent());
            }
        };
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PageResource.wizard.step.activation");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PageResource.wizard.step.activation.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PageResource.wizard.step.activation.subText");
    }
}
