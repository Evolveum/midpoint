/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

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
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
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
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeObjectTypeSuggestionNew;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

@PanelType(name = "rw-suggested-object-type")
@PanelInstance(identifier = "w-suggested-object-type",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ResourceObjectClassTableWizardPanel.headerLabel", icon = "fa fa-arrows-rotate"))
public abstract class ResourceSuggestedObjectTypeTableWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String ID_PANEL = "panel";

    private static final String OP_DETERMINE_STATUS =
            ResourceSuggestedObjectTypeTableWizardPanel.class.getName() + ".determineStatus";
    private static final String OP_DELETE_SUGGESTIONS =
            ResourceSuggestedObjectTypeTableWizardPanel.class.getName() + ".deleteSuggestions";

    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> selectedModel = Model.of();
    QName selectedObjectClassName;
    StatusInfo<ObjectTypesSuggestionType> statusInfo;

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
        SmartObjectTypeSuggestionTable<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> table = buildTableComponent();
        add(table);
    }

    private @NotNull SmartObjectTypeSuggestionTable<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> buildTableComponent() {
        LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> suggestionModel = createPrismContainerValueWrapperModel();
        ResourceDetailsModel assignmentHolderDetailsModel = getAssignmentHolderDetailsModel();
        ResourceType resource = assignmentHolderDetailsModel.getObjectType();
        SmartObjectTypeSuggestionTable<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> smartObjectTypeSuggestionTable
                = new SmartObjectTypeSuggestionTable<>(
                ID_PANEL,
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES_SUGGESTIONS,
                suggestionModel,
                selectedModel,
                resource.getOid()) {
            @Override
            public void refresh(AjaxRequestTarget target) {
                super.refresh(target);
                selectedModel = Model.of();
                SmartObjectTypeSuggestionTable<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> newTable =
                        buildTableComponent();
                newTable.setOutputMarkupId(true);

                ResourceSuggestedObjectTypeTableWizardPanel.this.addOrReplace(newTable);
                target.add(newTable);
            }
        };
        smartObjectTypeSuggestionTable.setOutputMarkupId(true);
        return smartObjectTypeSuggestionTable;
    }

    private @NotNull LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> createPrismContainerValueWrapperModel() {
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
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> suggestionValueWrapper = selectedModel.getObject();
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

        IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createContainerModel();
        ResourceObjectTypeDefinitionType suggestedValue = suggestionValueWrapper.getRealValue();
        PrismContainerValue<ResourceObjectTypeDefinitionType> originalObject =
                (PrismContainerValue<ResourceObjectTypeDefinitionType>) suggestedValue.asPrismContainerValue();
        WebPrismUtil.cleanupEmptyContainerValue(originalObject);

        PrismContainerValue<ResourceObjectTypeDefinitionType> suggestionToAdd = processSuggestedContainerValue(
                originalObject,
                containerModel.getObject().getItem());

        //TODO should be after save
        Task task = getPageBase().createSimpleTask(OP_DELETE_SUGGESTIONS);
        OperationResult result = task.getResult();
        removeObjectTypeSuggestionNew(getPageBase(), statusInfo, suggestedValue, task, result);

        onContinueWithSelected(selectedModel, suggestionToAdd, containerModel, target);
    }

    public <R extends Containerable> IModel<PrismContainerWrapper<R>> createContainerModel() {
        LoadableModel<PrismObjectWrapper<ResourceType>> objectWrapperModel = getAssignmentHolderDetailsModel()
                .getObjectWrapperModel();
        return PrismContainerWrapperModel.fromContainerWrapper(
                objectWrapperModel, ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
    }

    @Override
    protected String getSaveLabelKey() {
        return "ResourceObjectClassTableWizardPanel.saveButton";
    }

    protected abstract void onContinueWithSelected(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model,
            @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
            @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
            @NotNull AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.breadcrumbLabel");
    }

    @Override
    protected @Nullable IModel<String> getBreadcrumbIcon() {
        return Model.of("fa-solid fa-wand-magic-sparkles");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

}
