/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table.SmartObjectTypeSuggestionTable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

@PanelType(name = "rw-suggested-object-type")
@PanelInstance(identifier = "rw-suggested-object-type",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceSuggestedObjectTypeTableWizardPanel.headerLabel", icon = "fa fa-solid fa-wand-magic-sparkles"))
public abstract class ResourceSuggestedObjectTypeTableWizardPanel<P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String CLASS_DOT = ResourceSuggestedObjectTypeTableWizardPanel.class.getName() + ".";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + ".determineStatus";

    private static final String ID_PANEL = "panel";

    QName selectedObjectClassName;
    StatusInfo<ObjectTypesSuggestionType> statusInfo;
    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedModel = Model.of();

    public ResourceSuggestedObjectTypeTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            QName objectClassName) {
        super(id, superHelper);
        this.selectedObjectClassName = objectClassName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        initTable();
    }

    private void initTable() {

        var smartObjectTypeSuggestionTable = new SmartObjectTypeSuggestionTable<>(
                ID_PANEL,
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES_SUGGESTIONS,
                createValueWrapperModel(),
                selectedModel,
                getResourceOid()) {
            @Override
            protected @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getExistingObjectTypeDefinitions(
                    QName objectClassName) {
                return loadExistingAssociatedObjectTypes(objectClassName);
            }
        };
        smartObjectTypeSuggestionTable.setOutputMarkupId(true);
        add(smartObjectTypeSuggestionTable);
    }

    /**
     * Load existing object type definitions for the given object class name.
     */
    private @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> loadExistingAssociatedObjectTypes(
            QName objectClassName) {
        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> existingValues = new ArrayList<>();
        PrismObjectWrapper<ResourceType> objectWrapper = getAssignmentHolderDetailsModel().getObjectWrapper();
        try {
            PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = objectWrapper.findContainer(
                    ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> values = container.getValues();
            for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> valueWrapper : values) {
                ResourceObjectTypeDefinitionType realValue = valueWrapper.getRealValue();
                QName objectClass = realValue.getDelineation().getObjectClass();
                if (objectClassName.equals(objectClass)) {
                    existingValues.add(valueWrapper);
                }
            }
            return existingValues;
        } catch (SchemaException e) {
            throw new RuntimeException("Error retrieving existing object type definitions", e);
        }
    }

    private String getResourceOid() {
        ResourceDetailsModel assignmentHolderDetailsModel = getAssignmentHolderDetailsModel();
        return assignmentHolderDetailsModel.getObjectType().getOid();
    }

    private @NotNull LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> createValueWrapperModel() {
        return new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> load() {
                Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                ResourceType resource = getAssignmentHolderDetailsModel().getObjectType();

                statusInfo = loadObjectClassObjectTypeSuggestions(getPageBase(),
                        resource.getOid(),
                        selectedObjectClassName,
                        task,
                        result);

                if (statusInfo == null
                        || statusInfo.getResult() == null) {
                    return List.of();
                }

                ObjectTypesSuggestionType objectTypeSuggestionResult = statusInfo.getResult();

                if (objectTypeSuggestionResult.getObjectType() == null
                        || objectTypeSuggestionResult.getObjectType().isEmpty()) {
                    return List.of();
                }

                PrismContainerWrapper<ResourceObjectTypeDefinitionType> itemWrapper;
                try {
                    @SuppressWarnings("unchecked")
                    PrismContainerValue<ResourceObjectTypeDefinitionType> prismContainerValue = objectTypeSuggestionResult
                            .asPrismContainerValue();

                    PrismContainer<ResourceObjectTypeDefinitionType> container = prismContainerValue
                            .findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);
                    itemWrapper = getPageBase().createItemWrapper(
                            container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));
                } catch (SchemaException e) {
                    throw new RuntimeException("Error wrapping object type suggestions", e);
                }
                return itemWrapper.getValues();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        removeLastBreadcrumb();
        var suggestionValueWrapper = selectedModel.getObject();
        if (suggestionValueWrapper == null || suggestionValueWrapper.getRealValue() == null) {
            getPageBase().warn(getPageBase().createStringResource("Smart.suggestion.noSelection")
                    .getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        var suggestion = suggestionValueWrapper.getRealValue();
        var kind = suggestion.getKind();
        var intent = suggestion.getIntent();

        if (kind == null || intent == null || intent.isBlank()) {
            getPageBase().error(getPageBase().createStringResource("Smart.suggestion.missingKindIntent")
                    .getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        var originalObject = (PrismContainerValue<ResourceObjectTypeDefinitionType>) suggestionValueWrapper.getRealValue()
                .asPrismContainerValue();
        WebPrismUtil.cleanupEmptyContainerValue(originalObject);
        var suggestionToAdd = processSuggestedContainerValue(
                originalObject);

        onContinueWithSelected(selectedModel, suggestionToAdd, createContainerModel(), target);
    }

    public <R extends Containerable> IModel<PrismContainerWrapper<R>> createContainerModel() {
        var objectWrapperModel = getAssignmentHolderDetailsModel().getObjectWrapperModel();
        return PrismContainerWrapperModel.fromContainerWrapper(
                objectWrapperModel, ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
    }

    protected abstract void onContinueWithSelected(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model,
            @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
            @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
            @NotNull AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("ResourceSuggestedObjectTypeTableWizardPanel.breadcrumbLabel");
    }

    @Override
    protected @Nullable IModel<String> getBreadcrumbIcon() {
        return Model.of("fa-solid fa-wand-magic-sparkles");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("ResourceSuggestedObjectTypeTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("ResourceSuggestedObjectTypeTableWizardPanel.subText");
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        AjaxIconButton refreshSuggestionButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-refresh"),
                createStringResource("ResourceSuggestedObjectTypeTableWizardPanel.refreshSuggestionButton.title")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshSuggestionPerform(target);
            }
        };
        refreshSuggestionButton.showTitleAsLabel(true);
        refreshSuggestionButton.add(AttributeAppender.append("class", "btn btn-default"));
        buttons.add(refreshSuggestionButton);
    }

    public abstract void refreshSuggestionPerform(AjaxRequestTarget target);

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

    @Override
    protected String getSaveLabelKey() {
        return "ResourceSuggestedObjectTypeTableWizardPanel.review.selected";
    }

    @Override
    protected String getSubmitIcon() {
        return GuiStyleConstants.CLASS_ICON_SEARCH;
    }

    @Override
    protected String getSubmitButtonCssClass() {
        return "btn-primary";
    }

    @Override
    protected IModel<String> getExitLabel() {
        return createStringResource("SmartSuggestion.exitLabel");
    }

    @Override
    protected String getExitButtonCssClass() {
        return "btn-link mr-auto";
    }

    @Override
    protected String getButtonContainerAdditionalCssClass() {
        return "col-10";
    }

}
