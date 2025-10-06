/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import java.util.Collections;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.SimulationActionTaskButton;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceNavigationWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.mapping.SmartMappingTable;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadMappingTypeSuggestion;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_INBOUND_MAPPINGS;

/**
 * @author lskublik
 */
@PanelType(name = "rw-attribute-mappings")
@PanelInstance(identifier = "rw-attribute-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-attribute-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.outboundTable", icon = "fa fa-arrow-right-from-bracket"))
public abstract class AttributeMappingsTableWizardPanel<P extends Containerable> extends AbstractResourceNavigationWizardBasicPanel<P> {

    private static final String CLASS_DOT = AttributeMappingsTableWizardPanel.class.getName() + ".";
    private static final String OP_SUGGEST_MAPPING = CLASS_DOT + "suggestMapping";

    private static final String ID_AI_PANEL = "aiPanel";
    private static final String ID_PANEL = "panel";

    private final MappingDirection initialTab;

    public AttributeMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            MappingDirection initialTab) {
        super(id, superHelper);
        this.initialTab = initialTab;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<Boolean> switchToggleModel = Model.of(Boolean.FALSE);
        String resourceOid = getAssignmentHolderDetailsModel().getObjectType().getOid();

        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(ID_AI_PANEL, () -> {
            Task task = getPageBase().createSimpleTask("Load generation statusInfo");
            OperationResult result = task.getResult();
            LoadableModel<StatusInfo<?>> statusModel = new LoadableModel<>() {
                @Override
                protected StatusInfo<MappingsSuggestionType> load() {
                    return loadMappingTypeSuggestion(getPageBase(), resourceOid, task, result);
                }
            };
            return new SmartGeneratingAlertDto(statusModel, switchToggleModel, getPageBase());
        }) {
            @Override
            protected void performSuggestOperation(AjaxRequestTarget target) {
                PageBase pageBase = getPageBase();
                ResourceObjectTypeIdentification objectTypeIdentification = getResourceObjectTypeIdentification();
                SmartIntegrationService service = pageBase.getSmartIntegrationService();
                pageBase.taskAwareExecutor(target, OP_SUGGEST_MAPPING)
                        .runVoid((task, result) -> {
                            service.submitSuggestMappingsOperation(resourceOid, objectTypeIdentification, task, result);
                        });
            }

            @Override
            protected void refreshAssociatedComponents(@NotNull AjaxRequestTarget target) {
                SmartMappingTable<?> smartMappingTable = getSmartMappingTable();
                smartMappingTable.refreshAndDetach(target);
            }
        };

        aiPanel.setOutputMarkupId(true);
        add(aiPanel);

        SmartMappingTable<P> smartMappingTable = new SmartMappingTable<>(ID_PANEL,
                TABLE_SMART_INBOUND_MAPPINGS,
                Model.of(ViewToggle.TILE),
                Model.of(initialTab),
                switchToggleModel,
                getValueModel(),
                resourceOid) {
            @Override
            public void refreshAndDetach(AjaxRequestTarget target) {
                super.refreshAndDetach(target);
                //rerender also feedback panel
                target.add(getAiPanel());
                target.add(AttributeMappingsTableWizardPanel.this);
            }

            @Override
            public PrismContainerValueWrapper<MappingType> acceptSuggestionItemPerformed(
                    @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                    StatusInfo<MappingsSuggestionType> statusInfo,
                    @NotNull AjaxRequestTarget target) {
                PrismContainerValueWrapper<MappingType> newValue = createNewValue(rowModel.getObject().getNewValue(), target);
                deleteItemPerformed(target, Collections.singletonList(rowModel.getObject()), false);
                return newValue;
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel rowModel, boolean isDuplicate) {
                if (isInboundTabSelected) {
                    inEditInboundValue(rowModel, target);
                } else {
                    inEditOutboundValue(rowModel, target);
                }
            }
        };
        smartMappingTable.setOutputMarkupId(true);
        add(smartMappingTable);
    }

    boolean isInboundTabSelected = true;

    @SuppressWarnings("unchecked")
    protected SmartMappingTable<MappingType> getTable() {
        return ((SmartMappingTable<MappingType>) get(ID_PANEL));
    }

    public MappingDirection getSelectedMappingType() {
        return isInboundTabSelected ? MappingDirection.INBOUND : MappingDirection.OUTBOUND;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        IModel<PrismContainerValueWrapper<P>> valueModel = getValueModel();
        PrismContainerValueWrapper<P> object = valueModel.getObject();
        if (object.getRealValue() instanceof ResourceObjectTypeDefinitionType def) {
            buttons.add(createSimulationMenuButton(buttons, () -> def));
        }

        buttons.add(createShowOverridesButton(buttons));
    }

    private @NotNull AjaxIconButton createShowOverridesButton(@NotNull RepeatingView buttons) {
        AjaxIconButton showOverrides = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-shuffle"),
                getPageBase().createStringResource("AttributeMappingsTableWizardPanel.showOverrides")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getTable().isValidFormComponents(target)) {
                    onShowOverrides(target, getSelectedMappingType());
                }
            }
        };
        showOverrides.showTitleAsLabel(true);
        showOverrides.add(AttributeAppender.append("class", "btn  btn-outline-primary"));
        return showOverrides;
    }

    private @NotNull SimulationActionTaskButton createSimulationMenuButton(
            @NotNull RepeatingView buttons,
            @NotNull IModel<ResourceObjectTypeDefinitionType> objectTypeDefModel) {

        SimulationActionTaskButton simulationActionTaskButton = new SimulationActionTaskButton(
                buttons.newChildId(),
                objectTypeDefModel,
                () -> getAssignmentHolderDetailsModel().getObjectType()) {

            @Override
            protected ExecutionModeType getExecutionMode() {
                return ExecutionModeType.PREVIEW;
            }

            @Override
            public void redirectToSimulationTasksWizard(AjaxRequestTarget target) {
                AttributeMappingsTableWizardPanel.this.redirectToSimulationTasksWizard(target);
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getAdditionalSplitComponentCssClass() {
                return "ml-auto";
            }
        };

        simulationActionTaskButton.setRenderBodyOnly(true);
        return simulationActionTaskButton;
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected abstract void onShowOverrides(AjaxRequestTarget target, MappingDirection selectedMappingType);

    @Override
    protected String getSaveLabelKey() {
        return "AttributeMappingsTableWizardPanel.saveButton";
    }

    protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {

    }

    protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {

    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return Model.of();
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType) {
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }

    protected void redirectToSimulationTasksWizard(AjaxRequestTarget target) {

    }

    private @Nullable ResourceObjectTypeIdentification getResourceObjectTypeIdentification() {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper = findResourceObjectTypeDefinition();
        if (parentWrapper == null || parentWrapper.getRealValue() == null) {
            return null;
        }
        ResourceObjectTypeDefinitionType realValue = parentWrapper.getRealValue();
        return ResourceObjectTypeIdentification.of(realValue.getKind(), realValue.getIntent());
    }

    protected PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> findResourceObjectTypeDefinition() {
        return getValueModel().getObject()
                .getParentContainerValue(ResourceObjectTypeDefinitionType.class);
    }

    protected SmartMappingTable<?> getSmartMappingTable() {
        return (SmartMappingTable<?>) get(ID_PANEL);
    }

    protected SmartAlertGeneratingPanel getAiPanel() {
        return (SmartAlertGeneratingPanel) get(ID_AI_PANEL);
    }
}
