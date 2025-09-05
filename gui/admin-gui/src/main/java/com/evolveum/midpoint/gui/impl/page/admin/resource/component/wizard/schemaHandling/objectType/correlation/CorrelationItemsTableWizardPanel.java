/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeCorrelationTypeSuggestion;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.createMappingsValueIfRequired;
import static com.evolveum.midpoint.web.session.UserProfileStorage.TableId.TABLE_SMART_CORRELATION;

/**
 * @author lskublik
 */
@PanelType(name = "rw-correlationRules")
@PanelInstance(identifier = "rw-correlationRules",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationWizardPanelWizardPanel.headerLabel", icon = "fa fa-code-branch"))
public abstract class CorrelationItemsTableWizardPanel extends AbstractResourceWizardBasicPanel<CorrelationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemsTableWizardPanel.class);

    private static final String ID_NOT_SHOWN_CONTAINER_INFO = "notShownContainerInfo";
    private static final String PANEL_TYPE = "rw-correlationRules";
    private static final String ID_TABLE = "table";

    public CorrelationItemsTableWizardPanel(
            String id,
            WizardPanelHelper<CorrelationDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ResourceDetailsModel detailsModel = getHelper().getDetailsModel();
        ResourceType resource = detailsModel.getObjectType();
        String resourceOid = resource.getOid();

        SmartCorrelationTable table = createCorrelationTable(resourceOid);
        add(table);

        Label info = new Label(
                ID_NOT_SHOWN_CONTAINER_INFO,
                getPageBase().createStringResource("CorrelationItemsTableWizardPanel.notShownContainer"));
        info.setOutputMarkupId(true);
        info.add(new VisibleBehaviour(this::isNotShownContainerInfo));
        add(info);
    }

    private @NotNull SmartCorrelationTable createCorrelationTable(String resourceOid) {
        SmartCorrelationTable table = new SmartCorrelationTable(
                ID_TABLE,
                TABLE_SMART_CORRELATION,
                Model.of(ViewToggle.TILE),
                getValueModel().getObject(),
                resourceOid) {

            @Override
            protected MultivalueContainerListDataProvider<ItemsSubCorrelatorType> createProvider() {
                return super.createProvider();
            }

            @Override
            public void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    boolean isDuplicate) {

                if (rowModel == null) {
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(
                            getPageBase(), null, target);
                    showTableForItemRefs(target, this::findResourceObjectTypeDefinition, () -> newValue, null);
                    return;
                }

                if (isDuplicate && rowModel.getObject() != null) {
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> object = rowModel.getObject();
                    PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(
                            getPageBase(), object.getNewValue(), target);
                    showTableForItemRefs(target, this::findResourceObjectTypeDefinition, () -> newValue, null);
                }

                showTableForItemRefs(target, this::findResourceObjectTypeDefinition, rowModel, null);
            }

            @Override
            public void viewEditItemPerformed(AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    StatusInfo<CorrelationSuggestionsType> statusInfo) {
                if (rowModel.getObject() == null || rowModel.getObject().getRealValue() == null) {
                    return;
                }
                showTableForItemRefs(target, this::findResourceObjectTypeDefinition, rowModel, statusInfo);
            }

            @Override
            public void acceptSuggestionItemPerformed(AjaxRequestTarget target,
                    @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
                    StatusInfo<CorrelationSuggestionsType> statusInfo) {
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceObjectTypeDefinition =
                        findResourceObjectTypeDefinition();
                CorrelationItemsTableWizardPanel.this.acceptSuggestionItemPerformed(
                        getPageBase(), target, rowModel, () -> resourceObjectTypeDefinition, statusInfo);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void newItemPerformed(
                    PrismContainerValue<ItemsSubCorrelatorType> value,
                    AjaxRequestTarget target,
                    AssignmentObjectRelation relationSpec,
                    boolean isDuplicate,
                    StatusInfo<?> statusInfo) {
                PrismContainerValueWrapper<ItemsSubCorrelatorType> newValue = createNewItemsSubCorrelatorValue(getPageBase(), value, target);
                showTableForItemRefs(target, this::findResourceObjectTypeDefinition,
                        () -> newValue, (StatusInfo<CorrelationSuggestionsType>) statusInfo);
            }
        };

        table.setOutputMarkupId(true);
        return table;
    }

    protected void acceptSuggestionItemPerformed(PageBase pageBase, AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            StatusInfo<CorrelationSuggestionsType> statusInfo) {
        PrismContainerValueWrapper<CorrelationSuggestionType> parentSuggestionW = rowModel.getObject()
                .getParentContainerValue(CorrelationSuggestionType.class);

        if (statusInfo == null || parentSuggestionW == null || parentSuggestionW.getRealValue() == null) {
            pageBase.warn("Correlation suggestion is not available.");
            target.add(getPageBase().getFeedbackPanel().getParent());
            return;
        }

        CorrelationSuggestionType suggestion = parentSuggestionW.getRealValue();
        List<ResourceAttributeDefinitionType> attributes = suggestion.getAttributes();

        if (attributes.isEmpty()) {
            performAddOperation(pageBase, target, resourceObjectTypeDefinition, attributes, rowModel, statusInfo);
            return;
        }

        CorrelationAddMappingConfirmationPanel confirmationPanel = new CorrelationAddMappingConfirmationPanel(
                pageBase.getMainPopupBodyId(), Model.of(), () -> attributes) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                performAddOperation(pageBase, target, resourceObjectTypeDefinition, attributes, rowModel, statusInfo);
            }
        };
        pageBase.showMainPopup(confirmationPanel, target);
    }

    protected void performAddOperation(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDef,
            @Nullable List<ResourceAttributeDefinitionType> attributes,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo) {
        createMappingsValueIfRequired(pageBase, target, resourceObjectTypeDef, attributes);
        PrismContainerValueWrapper<ItemsSubCorrelatorType> object = valueModel.getObject();
        createNewItemsSubCorrelatorValue(pageBase, object.getNewValue(), target);
        performDiscard(pageBase, target, valueModel, statusInfo);
        postProcessAddSuggestion(target);
    }

    protected void postProcessAddSuggestion(AjaxRequestTarget target){
        getTable().refreshAndDetach(target);
    }

    protected void performDiscard(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<?> statusInfo) {
        Task task = pageBase.createSimpleTask("discardSuggestion");
        PrismContainerValueWrapper<CorrelationSuggestionType> parentContainerValue = valueModel.getObject()
                .getParentContainerValue(CorrelationSuggestionType.class);
        if (parentContainerValue != null && parentContainerValue.getRealValue() != null) {
            CorrelationSuggestionType suggestionToDelete = parentContainerValue.getRealValue();
            removeCorrelationTypeSuggestion(pageBase, statusInfo, suggestionToDelete, task, task.getResult());
        }
        target.add(this);
    }

    protected PrismContainerValueWrapper<ItemsSubCorrelatorType> createNewItemsSubCorrelatorValue(
            PageBase pageBase,
            PrismContainerValue<ItemsSubCorrelatorType> value,
            AjaxRequestTarget target) {
        return SmartIntegrationWrapperUtils.createNewItemsSubCorrelatorValue(
                getValueModel(), pageBase, value, target);
    }

    private @NotNull Boolean isNotShownContainerInfo() {
        PrismContainerValueWrapper<CorrelationDefinitionType> objectType = getValueModel().getObject();
        if (objectType != null) {
            try {
                PrismContainerWrapper<Containerable> correlators = objectType.findContainer(
                        ItemPath.create(CorrelationDefinitionType.F_CORRELATORS));
                if (correlators != null) {
                    PrismContainerValueWrapper<Containerable> correlatorsValue = correlators.getValue();
                    if (correlatorsValue != null) {
                        for (PrismContainerWrapper<? extends Containerable> container : correlatorsValue.getContainers()) {
                            if (container == null
                                    || container.isOperational()
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_ITEMS)
                                    || container.getItemName().equivalent(CompositeCorrelatorType.F_EXTENSION)) {
                                continue;
                            }

                            @SuppressWarnings("deprecation")
                            PrismContainer<? extends Containerable> cloneContainer = container.getItem().clone();
                            WebPrismUtil.cleanupEmptyContainers(cloneContainer);
                            if (!cloneContainer.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find correlators container in {}", objectType);
            }
        }
        return false;
    }

    protected abstract void showTableForItemRefs(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel,
            @Nullable StatusInfo<CorrelationSuggestionsType> statusInfo);

    @Override
    protected String getSaveLabelKey() {
        return "CorrelationWizardPanelWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationWizardPanelWizardPanel.subText");
    }

    protected SmartCorrelationTable getTable() {
        return (SmartCorrelationTable) get(ID_TABLE);
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
