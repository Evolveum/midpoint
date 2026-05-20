/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails.PARAM_PANEL_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceGuideObjectTypeTileState.computeState;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResult;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel.getSimulationResultReference;
import static com.evolveum.midpoint.schema.util.ShadowUtil.resolveDefault;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.WizardGuideTilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceAccountsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceEntitlementsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceGenericsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationActionTaskButton;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class ResourceObjectTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeWizardChoicePanel.class);

    private final WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper;

    public ResourceObjectTypeWizardChoicePanel(
            String id,
            @NotNull WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), ResourceObjectTypePreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-10 col-12 choice-tiles-container-8 gap-3 m-auto"));
    }

    public enum ResourceObjectTypePreviewTileType implements TileEnum {

        BASIC("fa fa-circle"),
        CORRELATION("fa fa-code-branch"),
        SYNCHRONIZATION("fa fa-arrows-rotate"),
        ATTRIBUTE_MAPPING("fa fa-retweet"),
        CAPABILITIES("fa fa-atom"),
        CREDENTIALS("fa fa-key"),
        ACTIVATION("fa fa-toggle-off"),
        POLICIES("fa fa-balance-scale");

        private final String icon;

        ResourceObjectTypePreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }

    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<ResourceObjectTypePreviewTileType>> tileModel) {
        ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();

        return new WizardGuideTilePanel<>(id, tileModel) {

            private @NotNull Boolean getDescription() {
                return StringUtils.isNotEmpty(tileModel.getObject().getDescription());
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                if (isLocked()) {
                    return;
                }

                Tile<ResourceObjectTypePreviewTileType> tile = tileModel.getObject();
                onTileClick(tile.getValue(), target);
            }

            @Override
            protected IModel<Badge> getBadgeModel() {
                ResourceObjectTypePreviewTileType tile = tileModel.getObject().getValue();
                ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();
                ResourceGuideObjectTypeTileState state = computeState(tile, resource, getValueModel(),
                        ResourceObjectTypeWizardChoicePanel.this);
                return state.badgeModel(ResourceObjectTypeWizardChoicePanel.this);
            }

            @Override
            protected IModel<String> getDescriptionTooltipModel() {
                ResourceObjectTypePreviewTileType tile = tileModel.getObject().getValue();

                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> wrapper = getValueModel().getObject();
                ResourceObjectTypeDefinitionType real = wrapper != null ? wrapper.getRealValue() : null;
                if (real == null) {
                    return null;
                }

                String key = ResourceGuideObjectTypeTileState.getTooltipKey(tile, resource, real);
                return key != null ? ResourceObjectTypeWizardChoicePanel.this.getPageBase().createStringResource(key) : null;
            }

            @Override
            protected boolean isLocked() {
                ResourceObjectTypePreviewTileType tile = tileModel.getObject().getValue();
                return computeState(tile, resource, getValueModel(),
                        ResourceObjectTypeWizardChoicePanel.this) == ResourceGuideObjectTypeTileState.TEMPORARY_LOCKED;
            }

            @Override
            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(this::getDescription);
            }
        };
    }

    @Override
    protected boolean addDefaultTile() {
        return false;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        buttons.add(createSimulationMenuButton(buttons));
        buttons.add(createPreviewDataButton(buttons.newChildId()));
        buttons.add(createGoToResourceButton(buttons.newChildId()));
    }

    private @NotNull AjaxIconButton createPreviewDataButton(String id) {
        AjaxIconButton button = new AjaxIconButton(
                id,
                Model.of("fa fa-magnifying-glass"),
                getPageBase().createStringResource("ResourceObjectTypePreviewTileType.PREVIEW_DATA")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showPreviewDataObjectType(target);
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeAppender.append("class", "btn btn-default"));
        return button;
    }

    private @NotNull AjaxIconButton createGoToResourceButton(String id) {
        AjaxIconButton button = new AjaxIconButton(
                id,
                Model.of("fa fa-arrow-right"),
                getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.goToResource")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToResourcePage();
            }
        };
        button.showTitleAsLabel(true);
        button.add(AttributeAppender.append("class", "btn btn-primary"));
        return button;
    }

    private void navigateToResourcePage() {
        ResourceObjectTypeDefinitionType objectType = getResourceObjectDefinition();
        ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();

        if (objectType == null || resource == null) {
            return;
        }

        ShadowKindType kind = objectType.getKind();
        storeSelectedObjectType(resource, kind, objectType.getIntent());

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, resource.getOid());
        parameters.add(PARAM_PANEL_ID, resolvePanelId(kind));

        Class<? extends PageBase> detailsPageClass = DetailsPageUtil.getObjectDetailsPage(ResourceType.class);
        getPageBase().removeLastBreadcrumb();
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private void storeSelectedObjectType(
            @NotNull ResourceType resource,
            @Nullable ShadowKindType kind,
            @Nullable String intent) {
        if (kind == null) {
            return;
        }

        ResourceContentStorage storage = getPageBase()
                .getBrowserTabSessionStorage()
                .getResourceContentStorage(kind);
        storage.getContentSearch().setResourceOid(resource.getOid());
        storage.getContentSearch().setIntent(resolveDefault(intent));
    }

    private @NotNull String resolvePanelId(@Nullable ShadowKindType kind) {
        if (ShadowKindType.GENERIC.equals(kind)) {
            return ResourceGenericsPanel.ID;
        }
        if (ShadowKindType.ACCOUNT.equals(kind)) {
            return ResourceAccountsPanel.ID;
        }
        if (ShadowKindType.ENTITLEMENT.equals(kind)) {
            return ResourceEntitlementsPanel.ID;
        }
        return ResourceUncategorizedPanel.ID;
    }

    private @NotNull SimulationActionTaskButton<?> createSimulationMenuButton(@NotNull RepeatingView buttons) {
        SimulationActionTaskButton<Void> simulationActionTaskButton = new SimulationActionTaskButton<>(
                buttons.newChildId(),
                this::getResourceObjectDefinition,
                () -> getAssignmentHolderDetailsModel().getObjectType()) {
            @Override
            protected StringResourceModel getTitleModel(@NotNull PageBase pageBase) {
                return pageBase.createStringResource("ResourceObjectTypeWizardChoicePanel.simulation");
            }

            @Override
            protected void onShowResultProcess(AjaxRequestTarget target, TaskType task, PageBase pageBase) {
                ObjectReferenceType simulationResultReference = getSimulationResultReference(task);
                if (simulationResultReference == null || simulationResultReference.getOid() == null) {
                    LOGGER.error("Simulation result reference or OID is null for task {}", task.getName());
                    return;
                }
                SimulationResultType simulationResultType = loadSimulationResult(pageBase, simulationResultReference.getOid());
                buildSimulationResultPanel(target, Model.of(simulationResultType));
            }

            @Override
            public void redirectToSimulationTasksWizard(AjaxRequestTarget target) {
                ResourceObjectTypeWizardChoicePanel.this.redirectToSimulationTasksWizard(target);
            }

            @Override
            protected @NotNull ResourceTaskFlavor<Void> getTaskFlavor() {
                return ResourceTaskFlavors.RECONCILIATION;
            }

            @Override
            protected ExecutionModeType getExecutionMode() {
                return ExecutionModeType.PREVIEW;
            }
        };

        simulationActionTaskButton.setRenderBodyOnly(true);
        return simulationActionTaskButton;
    }

    protected void buildSimulationResultPanel(AjaxRequestTarget target, IModel<SimulationResultType> simulationResultTypeIModel) {

    }

    protected void showPreviewDataObjectType(AjaxRequestTarget target) {
    }

    @Override
    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return helper.getValueModel();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {

                if (getValueModel() == null
                        || getValueModel().getObject() == null
                        || getValueModel().getObject().getRealValue() == null) {
                    return translate("ResourceObjectTypeWizardPreviewPanel.breadcrumb");
                }

                return GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
            }
        };
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.text");
    }

    protected void redirectToSimulationTasksWizard(AjaxRequestTarget target) {

    }

    private @Nullable ResourceObjectTypeDefinitionType getResourceObjectDefinition() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = getValueModel().getObject();
        return object != null ? object.getRealValue() : null;
    }

    @Override
    protected String getButtonContainerAdditionalCssClass() {
        return "col-12 col-sm-8";
    }
}
