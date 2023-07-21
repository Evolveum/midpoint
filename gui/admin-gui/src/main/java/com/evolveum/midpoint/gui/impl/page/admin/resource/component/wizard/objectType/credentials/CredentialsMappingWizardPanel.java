/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

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

@PanelType(name = "rw-credentials")
@PanelInstance(identifier = "rw-credentials",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.credentials", icon = "fa fa-toggle-off"))
public abstract class CredentialsMappingWizardPanel extends AbstractSpecificMappingWizardPanel<ResourceCredentialsDefinitionType> {

    public static final String PANEL_TYPE = "rw-credentials";

    public CredentialsMappingWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerWrapper<ResourceCredentialsDefinitionType>> containerModel,
            MappingDirection initialTab) {
        super(id, model, containerModel, initialTab);
    }

    @Override
    protected AbstractSpecificMappingTileTable<ResourceCredentialsDefinitionType> createTablePanel(
            String panelId,
            IModel<PrismContainerWrapper<ResourceCredentialsDefinitionType>> containerModel,
            MappingDirection mappingDirection) {
        return new CredentialsMappingTileTable(panelId, containerModel, mappingDirection, getAssignmentHolderDetailsModel()) {
            @Override
            protected void editPredefinedMapping(
                    IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
                    AjaxRequestTarget target) {
                CredentialsMappingWizardPanel.this.editPredefinedMapping(valueModel, mappingDirection, target);
            }

            @Override
            protected void editConfiguredMapping(
                    IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target) {
                CredentialsMappingWizardPanel.this.editConfiguredMapping(valueModel, mappingDirection, target);
            }

            @Override
            public void refresh(AjaxRequestTarget target) {
                super.refresh(target);
                target.add(getParent());
            }
        };
    }

    protected abstract void editOutboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target);

    protected abstract void editInboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target);

    @Override
    protected void editPredefinedMapping(
            IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
            MappingDirection direction,
            AjaxRequestTarget target) {
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PageResource.wizard.step.credentials");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PageResource.wizard.step.credentials.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PageResource.wizard.step.credentials.subText");
    }
}
