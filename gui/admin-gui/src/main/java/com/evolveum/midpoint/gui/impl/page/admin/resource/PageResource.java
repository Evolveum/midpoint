/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.basic.FocusResourceObjectTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.page.admin.resources.ResourceSummaryPanel;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resource")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        label = "PageAdminResources.auth.resourcesAll.label",
                        description = "PageAdminResources.auth.resourcesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                        label = "PageResource.auth.resource.label",
                        description = "PageResource.auth.resource.description")
        })
public class PageResource extends PageAssignmentHolderDetails<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

//    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
//    private static final String ID_WIZARD = "wizard";


    public PageResource(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageResource(PrismObject<ResourceType> resource) {
        super(resource);
    }

    @Override
    public Class<ResourceType> getType() {
        return ResourceType.class;
    }

    protected boolean isApplicableTemplate() {
        return true;
    }

    protected WebMarkupContainer createTemplatePanel(String id) {
        setShowedByWizard(true);
        getObjectDetailsModels().reset();
        return new ResourceWizardPanel(id, createObjectWizardPanelHelper());
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ResourceType> summaryModel) {
        return new ResourceSummaryPanel(id,
                summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ResourceOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> wrapperModel) {
        return new ResourceOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void refreshStatus(AjaxRequestTarget target) {
                target.add(PageResource.this.get(ID_DETAILS_VIEW));
                PageResource.this.refresh(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageResource.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageResource.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected ResourceDetailsModel createObjectDetailsModels(PrismObject<ResourceType> object) {
        return new ResourceDetailsModel(createPrismObjectModel(object), this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        GetOperationOptionsBuilder builder = getOperationOptionsBuilder()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .item(
                        ItemPath.create(
                                ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_FOCUS,
                                ResourceObjectFocusSpecificationType.F_ARCHETYPE_REF)).resolve();

        if (useNoFetchOption()) {
            builder.noFetch();
        }
        return builder.build();
    }

    private boolean useNoFetchOption() {
        ResourceType resource = getObjectDetailsModels().getObjectType();
        return resource == null || StringUtils.isNotEmpty(resource.getOid());
    }

    public void showResourceObjectTypeBasicWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceObjectTypeBasicWizardPanel wizardPanel = showWizard(target, pathToValue, ResourceObjectTypeBasicWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showResourceObjectTypePreviewWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceObjectTypeWizardPanel wizard = showObjectTypeWizard(target, pathToValue);
        wizard.setShowObjectTypePreview(true);
    }

    public void showSynchronizationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        SynchronizationWizardPanel wizardPanel = showWizard(target, pathToValue, SynchronizationWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showCorrelationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        CorrelationWizardPanel wizardPanel = showWizard(target, pathToValue, CorrelationWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showCapabilitiesWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        CapabilitiesWizardPanel wizardPanel = showWizard(target, pathToValue, CapabilitiesWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showCredentialsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        CredentialsWizardPanel wizardPanel = showWizard(target, pathToValue, CredentialsWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showActivationsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ActivationsWizardPanel wizardPanel = showWizard(target, pathToValue, ActivationsWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public void showAssociationsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        AssociationsWizardPanel wizardPanel = showWizard(target, pathToValue, AssociationsWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    public ResourceObjectTypeWizardPanel showObjectTypeWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceObjectTypeWizardPanel wizard =
                showWizard(target, pathToValue, ResourceObjectTypeWizardPanel.class);
        return wizard;
    }

    public void showAttributeMappingWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        AttributeMappingWizardPanel wizardPanel = showWizard(target, pathToValue, AttributeMappingWizardPanel.class);
        addWizardBreadcrumbsForObjectType(wizardPanel);
    }

    private void addWizardBreadcrumbsForObjectType(
            AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> wizardPanel) {
        List<Breadcrumb> breadcrumbs = getWizardBreadcrumbs();
        ResourceObjectTypeDefinitionType objectType = wizardPanel.getValueModel().getObject().getRealValue();
        String displayName = GuiDisplayNameUtil.getDisplayName(objectType);
        IModel<String> breadcrumbLabelModel = Model.of(displayName);

            breadcrumbs.add(0, new Breadcrumb(breadcrumbLabelModel));
    }
}
