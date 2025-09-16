/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schema.ResourceSchemaWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.ResourceAssociationTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.AssociationInboundEvaluatorWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.AssociationInboundMappingContainerWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.AssociationOutboundEvaluatorWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.AssociationOutboundMappingContainerWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.PoliciesObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.SmartObjectTypeSuggestionWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;
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

import org.jetbrains.annotations.NotNull;

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
        return false;
    }

    @Override
    protected boolean canShowWizard() {
        return isAdd();
    }

    @Override
    protected DetailsFragment createWizardFragment() {
        getObjectDetailsModels().reset();
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageResource.this) {
            @Override
            protected void initFragmentLayout() {
                add(new ResourceWizardPanel(ID_TEMPLATE, createObjectWizardPanelHelper()));
            }
        };

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
            protected void submitPerformed(AjaxRequestTarget target) {
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
                                ResourceObjectFocusSpecificationType.F_ARCHETYPE_REF)).resolve()
                .item(
                        ItemPath.create(
                                ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_DEFAULT_OPERATION_POLICY,
                                DefaultOperationPolicyConfigurationType.F_POLICY_REF)).resolve()
                .item(
                        ItemPath.create(
                                ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_MARKING,
                                ShadowMarkingConfigurationType.F_MARK_REF)).resolve();

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
        ResourceObjectTypeWizardPanel wizard = showObjectTypeWizard(null, target, pathToValue);
        wizard.setShowChoicePanel(true);
    }

    public ResourceObjectTypeWizardPanel showObjectTypeWizard(PrismContainerValue<ResourceObjectTypeDefinitionType> value, AjaxRequestTarget target, ItemPath pathToValue) {
        return showWizard(value, target, pathToValue, ResourceObjectTypeWizardPanel.class);
    }

    public void showResourceAssociationTypePreviewWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceAssociationTypeWizardPanel wizard = showAssociationTypeWizard(null, target, pathToValue);
        wizard.setShowChoicePanel(true);
    }

    public ResourceAssociationTypeWizardPanel showAssociationTypeWizard(PrismContainerValue<ShadowAssociationTypeDefinitionType> value, AjaxRequestTarget target, ItemPath pathToValue) {
        return showWizard(value, target, pathToValue, ResourceAssociationTypeWizardPanel.class);
    }

    public ResourceAssociationTypeWizardPanel showAssociationTypeWizardForDuplicate(PrismContainerValue<ShadowAssociationTypeDefinitionType> value, AjaxRequestTarget target, ItemPath pathToValue) {
        ResourceAssociationTypeWizardPanel panel = showWizard(value, target, pathToValue, ResourceAssociationTypeWizardPanel.class);
        panel.setPanelForDuplicate(true);
        return panel;
    }

    public ResourceAssociationTypeWizardPanel showAssociationTypeWizard(AjaxRequestTarget target, IModel<String> exitLabel) {
        return showWizard(target, ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_ASSOCIATION_TYPE), ResourceAssociationTypeWizardPanel.class, exitLabel);
    }

    public void showResourceObjectTypeBasicWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue, ResourceObjectTypeBasicWizardPanel.class);
    }

    public void showSynchronizationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue.append(ResourceObjectTypeDefinitionType.F_SYNCHRONIZATION), SynchronizationWizardPanel.class);
    }

    public void showSuggestObjectTypeWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectTypeSuggestion(target, pathToValue, SmartObjectTypeSuggestionWizardPanel.class);
    }

    public void showAttributeMappingWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue, AttributeMappingWizardPanel.class);
    }

    public void showCorrelationWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerCorrelationWizard(target, pathToValue, CorrelationWizardPanel.class);
    }

    public void showCapabilitiesWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue.append(ResourceObjectTypeDefinitionType.F_CONFIGURED_CAPABILITIES), CapabilitiesWizardPanel.class);
    }

    public void showCredentialsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue.append(ResourceObjectTypeDefinitionType.F_CREDENTIALS), CredentialsWizardPanel.class);
    }

    public void showActivationsWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue.append(ResourceObjectTypeDefinitionType.F_ACTIVATION), ActivationsWizardPanel.class);
    }

    public void showPoliciesWizard(AjaxRequestTarget target, ItemPath pathToValue) {
        showContainerWizardForObjectType(target, pathToValue, PoliciesObjectTypeWizardPanel.class);
    }

    public void showAssociationInboundsWizard(AjaxRequestTarget target, ItemPath pathToValue, ShadowAssociationTypeDefinitionType association, IModel<String> exitLabel) {
        showWizard(target, pathToValue, AssociationInboundMappingContainerWizardPanel.class, exitLabel);
        addWizardBreadcrumbsForAssociationType(association, LocalizationUtil.translate("PageResource.association.wizard.inbound"));
    }

    public void showAssociationInboundWizard(AjaxRequestTarget target, ItemPath pathToValue, ShadowAssociationTypeDefinitionType association, IModel<String> exitLabel) {
        AssociationInboundEvaluatorWizardPanel panel = showWizard(target, pathToValue, AssociationInboundEvaluatorWizardPanel.class, exitLabel);
        panel.setShowChoicePanel(true);
        addWizardBreadcrumbsForAssociationType(association, LocalizationUtil.translate("PageResource.association.wizard.inbound"));
    }

    public void showAssociationOutboundsWizard(AjaxRequestTarget target, ItemPath pathToValue, ShadowAssociationTypeDefinitionType association, IModel<String> exitLabel) {
        showWizard(target, pathToValue, AssociationOutboundMappingContainerWizardPanel.class, exitLabel);
        addWizardBreadcrumbsForAssociationType(association, LocalizationUtil.translate("PageResource.association.wizard.outbound"));
    }

    public void showAssociationOutboundWizard(AjaxRequestTarget target, ItemPath pathToValue, ShadowAssociationTypeDefinitionType association, IModel<String> exitLabel) {
        AssociationOutboundEvaluatorWizardPanel panel = showWizard(target, pathToValue, AssociationOutboundEvaluatorWizardPanel.class, exitLabel);
        panel.setShowChoicePanel(true);
        addWizardBreadcrumbsForAssociationType(association, LocalizationUtil.translate("PageResource.association.wizard.outbound"));
    }

    public ResourceSchemaWizardPanel showComplexOrEnumerationTypeWizard(AjaxRequestTarget target) {
        return showWizard(
                target,
                ItemPath.EMPTY_PATH,
                ResourceSchemaWizardPanel.class);
    }

    private <P extends AbstractWizardPanel> P showContainerWizardForObjectType(
            AjaxRequestTarget target, ItemPath pathToValue, Class<P> wizardClass) {
        P wizardPanel = (P) showWizard(target, pathToValue, wizardClass);
        addWizardBreadcrumbsForObjectType(wizardPanel, 0);
        return wizardPanel;
    }

    private <P extends AbstractWizardPanel> P showContainerCorrelationWizard(
            AjaxRequestTarget target, @NotNull ItemPath pathToValue, Class<P> wizardClass) {
        pathToValue.append(ResourceObjectTypeDefinitionType.F_CORRELATION);
        P wizardPanel = (P) showWizard(target, pathToValue, wizardClass);
        addWizardBreadcrumbsResourceName(wizardPanel);
        return wizardPanel;
    }

    private <P extends AbstractWizardPanel> P showContainerWizardForObjectTypeSuggestion(
            AjaxRequestTarget target, ItemPath pathToValue, Class<P> wizardClass) {
        P wizardPanel = (P) showWizard(target, pathToValue, wizardClass);
        addWizardBreadcrumbsResourceName(wizardPanel);
        addWizardBreadcrumbsForObjectType(wizardPanel, 1);
        return wizardPanel;
    }

    private <C extends Containerable> void addWizardBreadcrumbsForObjectType(
            @NotNull AbstractWizardPanel<C, ResourceDetailsModel> wizardPanel, int index) {
        List<Breadcrumb> breadcrumbs = getWizardBreadcrumbs();
        Containerable containerable = wizardPanel.getValueModel().getObject().getRealValue();
        String displayName = "";
        if (containerable instanceof ResourceObjectTypeDefinitionType objectType) {
            displayName = GuiDisplayNameUtil.getDisplayName(objectType);

        } else if (containerable != null) {
            PrismContainerValue<?> prismContainerValue = containerable.asPrismContainerValue();
            displayName = GuiDisplayNameUtil.getDisplayName(prismContainerValue);
        }

        IModel<String> breadcrumbLabelModel = Model.of(displayName);
        breadcrumbs.add(index, new Breadcrumb(breadcrumbLabelModel));
    }

    private <C extends Containerable> void addWizardBreadcrumbsResourceName(
            @NotNull AbstractWizardPanel<C, ResourceDetailsModel> wizardPanel) {
        List<Breadcrumb> breadcrumbs = getWizardBreadcrumbs();
        PolyStringType resourceName = wizardPanel.getAssignmentHolderModel().getObjectType().getName();
        IModel<String> breadcrumbLabel = resourceName != null
                ? Model.of(resourceName.getOrig())
                : createStringResource("PageResource.resource.breadcrumb");
        breadcrumbs.add(0, new Breadcrumb(breadcrumbLabel));
    }

    private void addWizardBreadcrumbsForAssociationType(ShadowAssociationTypeDefinitionType association, String mappingDirection) {
        List<Breadcrumb> breadcrumbs = getWizardBreadcrumbs();
        String displayName = GuiDisplayNameUtil.getDisplayName(association);
        IModel<String> breadcrumbLabelModel = Model.of(displayName + " (" + mappingDirection + ")");

        breadcrumbs.add(0, new Breadcrumb(breadcrumbLabelModel));
    }
}
