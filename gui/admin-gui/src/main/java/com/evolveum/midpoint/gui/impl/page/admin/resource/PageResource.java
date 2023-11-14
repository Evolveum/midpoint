/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.resources.ResourceSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectFocusSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

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

    public void showResourceObjectTypePreviewWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceObjectTypeWizardPanel wizard = showObjectTypeWizard(target, pathToValue);
        wizard.setShowObjectTypePreview(true);
    }

    public ResourceObjectTypeWizardPanel showObjectTypeWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceObjectTypeWizardPanel wizard =
                showWizard(target, pathToValue, ResourceObjectTypeWizardPanel.class);
        return wizard;
    }

    public void showResourceObjectTypeBasicWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, ResourceObjectTypeBasicWizardPanel.class);
    }

    public void showSynchronizationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, SynchronizationWizardPanel.class);
    }

    public void showAttributeMappingWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, AttributeMappingWizardPanel.class);
    }

    public void showCorrelationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, CorrelationWizardPanel.class);
    }

    public void showCapabilitiesWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, CapabilitiesWizardPanel.class);
    }

    public void showCredentialsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, CredentialsWizardPanel.class);
    }

    public void showActivationsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, ActivationsWizardPanel.class);
    }

    public void showAssociationsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizard(target, pathToValue, AssociationsWizardPanel.class);
    }

    private <P extends AbstractWizardPanel> void showContainerWizard(
            AjaxRequestTarget target, ItemPath pathToValue, Class<P> wizardClass) {
        P wizardPanel = (P) showWizard(target, pathToValue, wizardClass);
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
