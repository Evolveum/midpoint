/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.loadObjectTypeSuggestions;

@PanelType(name = "resourceObjectTypes")
@PanelInstance(identifier = "resourceObjectTypes", applicableForType = ResourceType.class,
        childOf = SchemaHandlingPanel.class,
        display = @PanelDisplay(label = "PageResource.tab.objectTypes", icon = GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, order = 10))
public class ResourceObjectTypesPanel extends SchemaHandlingObjectsPanel<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypesPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ItemPath getTypesContainerPath() {
        return ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "ResourceSchemaHandlingPanel.newObject";
    }

    public List<StatusInfo<ObjectTypesSuggestionType>> getSuggestions() {
        Task task = getPageBase().createSimpleTask("Loading object type suggestions");
        OperationResult result = task.getResult();
        ResourceType resource = getObjectDetailsModels().getObjectType();

        return loadObjectTypeSuggestions(
                getPageBase(), resource.getOid(), task, result);
    }

    private void addSuggestedObjectTypesValueWrappers(PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceTypeCPrismContainerWrapperModel) {
        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> prismContainerValueWrappers = loadObjectTypesSuggestionValueWrappers();

        for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> prismContainerValueWrapper : prismContainerValueWrappers) {
            resourceTypeCPrismContainerWrapperModel.getObject().getValues().add(prismContainerValueWrapper);
        }
    }

    Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, StatusInfo<ObjectTypesSuggestionType>> suggestionMap = new HashMap<>();

    protected List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> loadObjectTypesSuggestionValueWrappers() {
        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> values = new ArrayList<>();

        Task task = getPageBase().createSimpleTask("Loading object type suggestions");
        OperationResult result = task.getResult();

        List<StatusInfo<ObjectTypesSuggestionType>> suggestions = getSuggestions();

        if (suggestions == null || suggestions.isEmpty()) {
            return values;
        }
        for (StatusInfo<ObjectTypesSuggestionType> statusInfo : suggestions) {

            if (statusInfo == null) {
                continue;
            }

            ObjectTypesSuggestionType objectTypeSuggestionResult = new ObjectTypesSuggestionType();
            if (statusInfo.getResult() != null) {
                objectTypeSuggestionResult = statusInfo.getResult();
            }

            List<ObjectTypeSuggestionType> suggestedObjectType = objectTypeSuggestionResult.getObjectType();

            if (suggestedObjectType.isEmpty()) {
                suggestedObjectType.add(new ObjectTypeSuggestionType());
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
                List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueWrapper = itemWrapper.getValues();

                for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> cPrismContainerValueWrapper : valueWrapper) {
                    suggestionMap.put(cPrismContainerValueWrapper, statusInfo);
                    values.add(cPrismContainerValueWrapper);
                }

            } catch (SchemaException e) {
                throw new RuntimeException("Error wrapping object type suggestions", e);
            }
        }

        return values;
    }

//    @Override
//    protected Component onNameColumnPopulateItem(
//            Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
//            String componentId,
//            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
//        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = rowModel.getObject();
//        StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionMap.get(object);
//        if (suggestionTypeStatusInfo != null) {
//            ResourceObjectTypeDefinitionType objectRealValue = object.getRealValue();
//
//            String displayName = objectRealValue.getDisplayName();
//            Label label = new Label(componentId, Model.of(displayName != null ? displayName : ""));
//            label.setOutputMarkupId(true);
//            return label;
//        }
//        return super.onNameColumnPopulateItem(cellItem, componentId, rowModel);
//
//    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> columns = new ArrayList<>();
        LoadableDetachableModel<PrismContainerDefinition<ResourceObjectTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceObjectTypeDefinitionType> load() {
                PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceTypeCPrismContainerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

                //add suggested object types
                addSuggestedObjectTypesValueWrappers(resourceTypeCPrismContainerWrapperModel);

                return resourceTypeCPrismContainerWrapperModel.getObject();
            }
        };

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_OBJECT_CLASS,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = rowModel.getObject();
                StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionMap.get(object);

                if (suggestionTypeStatusInfo == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                    return;
                }
                QName objectClassName = suggestionTypeStatusInfo.getObjectClassName();
                Label label = new Label(componentId, Model.of(objectClassName != null ? objectClassName.getLocalPart() : ""));
                label.setOutputMarkupId(true);
                cellItem.add(label);

            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_KIND,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_INTENT,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_DEFAULT,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new LifecycleStateColumn<>(defModel, getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = rowModel.getObject();
                StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionMap.get(object);

                if (suggestionTypeStatusInfo == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                    return;
                }

                OperationResultStatusType status = suggestionTypeStatusInfo.getStatus();

                Label statusLabel = new Label(componentId, Model.of(status.value()));
                statusLabel.setOutputMarkupId(true);
                cellItem.add(statusLabel);
            }
        });

        return columns;
    }

    @Override
    protected Class<ResourceObjectTypeDefinitionType> getSchemaHandlingObjectsType() {
        return ResourceObjectTypeDefinitionType.class;
    }

    @Override
    protected void onNewValue(
            PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target, boolean isDuplicate) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showObjectTypeWizard(value, target, newWrapperModel.getObject().getPath());
    }

    @Override
    protected void onSuggestValue(PrismContainerValue<ResourceObjectTypeDefinitionType> value, IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showSuggestObjectTypeWizard(target, createContainerModel().getObject().getPath());
    }

    @Override
    protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
        if (valueModel != null) {
            getObjectDetailsModels().getPageResource().showResourceObjectTypePreviewWizard(
                    target,
                    valueModel.getObject().getPath());
        }
    }

    @Override
    protected boolean allowNoValuePanel() {
        return true;
    }
}
