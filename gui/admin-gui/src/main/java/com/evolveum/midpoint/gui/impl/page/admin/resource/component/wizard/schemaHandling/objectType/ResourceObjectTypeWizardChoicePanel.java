/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.WizardGuideTilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.ObjectTypeStatisticsButton;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.SimulationActionTaskButton;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceGuideObjectTypeTileState.computeState;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil.loadSimulationResult;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel.getSimulationResultReference;

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
                ResourceGuideObjectTypeTileState state = computeState(tile, getValueModel(),
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

                String key = ResourceGuideObjectTypeTileState.getTooltipKey(tile, real);
                return key != null ? ResourceObjectTypeWizardChoicePanel.this.getPageBase().createStringResource(key) : null;
            }

            @Override
            protected boolean isLocked() {
                ResourceObjectTypePreviewTileType tile = tileModel.getObject().getValue();
                return computeState(tile, getValueModel(),
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
        SimulationActionTaskButton<?> simulationActionTaskButton = createSimulationMenuButton(buttons);
        buttons.add(simulationActionTaskButton);

        initObjectTypeStatisticsButton(buttons);

        AjaxIconButton previewData = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-magnifying-glass"),
                getPageBase().createStringResource("ResourceObjectTypePreviewTileType.PREVIEW_DATA")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showPreviewDataObjectType(target);
            }
        };
        previewData.showTitleAsLabel(true);
        previewData.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(previewData);

    }

    private void initObjectTypeStatisticsButton(@NotNull RepeatingView buttons) {
        IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel = getValueModel();
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = valueModel.getObject();
        ResourceObjectTypeDefinitionType realValue = object.getRealValue();
        ShadowKindType kind = realValue.getKind();
        String intent = realValue.getIntent();

        ResourceObjectTypeIdentification objectTypeIdentification = ResourceObjectTypeIdentification.of(kind, intent);

        ObjectTypeStatisticsButton statisticsButton = new ObjectTypeStatisticsButton(
                buttons.newChildId(),
                () -> objectTypeIdentification,
                getAssignmentHolderDetailsModel().getObjectType().getOid());
        statisticsButton.setOutputMarkupId(true);
        statisticsButton.setRenderBodyOnly(true);
        buttons.add(statisticsButton);
    }

    private @NotNull SimulationActionTaskButton<?> createSimulationMenuButton(@NotNull RepeatingView buttons) {
        SimulationActionTaskButton<Void> simulationActionTaskButton = new SimulationActionTaskButton<>(
                buttons.newChildId(),
                this::getResourceObjectDefinition,
                () -> getAssignmentHolderDetailsModel().getObjectType()) {

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
                return ResourceTaskFlavors.IMPORT;
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
}
