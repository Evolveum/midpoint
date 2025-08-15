/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.loadObjectTypeSuggestions;

@PanelType(name = "resourceObjectTypes")
@PanelInstance(identifier = "resourceObjectTypes", applicableForType = ResourceType.class,
        childOf = SchemaHandlingPanel.class,
        display = @PanelDisplay(label = "PageResource.tab.objectTypes", icon = GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, order = 10))
public class ResourceObjectTypesPanel extends SchemaHandlingObjectsPanel<ResourceObjectTypeDefinitionType> {

    private static final String OP_DETERMINE_STATUSES =
            ResourceObjectTypesPanel.class.getName() + ".determineStatuses";

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

    // Holds the "why" for each rendered wrapper.
    private final Map<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>,
            StatusInfo<ObjectTypesSuggestionType>> suggestionByWrapper = new HashMap<>();

    /** Loads object-type suggestions for the current resource. */
    public List<StatusInfo<ObjectTypesSuggestionType>> getSuggestions() {
        final Task task = getPageBase().createSimpleTask("Load object type suggestions");
        final OperationResult result = task.getResult();

        final ResourceType resource = getObjectDetailsModels().getObjectType();
        if (resource == null || resource.getOid() == null) {
            return Collections.emptyList();
        }

        return loadObjectTypeSuggestions(getPageBase(), resource.getOid(), task, result);
    }

    /** Creates value wrappers for each suggested object type. */
    protected List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> loadSuggestionWrappers() {
        final List<StatusInfo<ObjectTypesSuggestionType>> suggestions = getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        final OperationResult result = task.getResult();

        final List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> out = new ArrayList<>();

        for (StatusInfo<ObjectTypesSuggestionType> si : suggestions) {
            if (si == null) {
                continue; // defensive
            }

            // Ensure a non-null suggestion object.
            ObjectTypesSuggestionType suggestion =
                    si.getResult() != null ? si.getResult() : new ObjectTypesSuggestionType();

            // Guarantee at least one object type entry exists so we can wrap it.
            List<ResourceObjectTypeDefinitionType> objectTypes = suggestion.getObjectType();
            if (objectTypes.isEmpty()) {
                objectTypes.add(new ResourceObjectTypeDefinitionType());
            }

            try {
                @SuppressWarnings("unchecked")
                PrismContainerValue<ObjectTypesSuggestionType> suggestionPcv =
                        suggestion.asPrismContainerValue();

                PrismContainer<ResourceObjectTypeDefinitionType> container =
                        suggestionPcv.findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);

                PrismContainerWrapper<ResourceObjectTypeDefinitionType> wrapper =
                        getPageBase().createItemWrapper(
                                container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));

                for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> v : wrapper.getValues()) {
                    suggestionByWrapper.put(v, si);
                    out.add(v);
                }
            } catch (SchemaException e) {
                throw new IllegalStateException("Failed to wrap object type suggestions", e);
            }
        }

        return out;
    }

    @Override
    protected Component onNameColumnPopulateItem(
            Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
            String componentId,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = rowModel.getObject();
        StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionByWrapper.get(object);
//        if (suggestionTypeStatusInfo != null) {
//
//        }
        return super.onNameColumnPopulateItem(cellItem, componentId, rowModel);

    }

    @Override
    protected void customizeNewRowItem(
            Item<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> item,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object = model.getObject();
        StatusInfo<ObjectTypesSuggestionType> objectTypesSuggestionTypeStatusInfo = suggestionByWrapper.get(object);

        if (objectTypesSuggestionTypeStatusInfo != null) {
            OperationResultStatusType status = objectTypesSuggestionTypeStatusInfo.getStatus();
            switch (status) {
                case FATAL_ERROR -> item.add(AttributeModifier.append("class", "bg-light-danger"));
                case IN_PROGRESS -> item.add(AttributeModifier.append("class", "bg-light-info"));
                default -> item.add(AttributeModifier.append("class", "bg-light-purple"));
            }
        } else {
            super.customizeNewRowItem(item, model);
        }
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createProvider() {
        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceDefWrapper = PrismContainerWrapperModel
                .fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        LoadableModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> containerModel = new LoadableModel<>() {
            @Override
            protected @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> load() {
                List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueContainer = new ArrayList<>();

                List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> suggestionWrappers = loadSuggestionWrappers();
                List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypesWrapper = resourceDefWrapper
                        .getObject().getValues();

                if (suggestionWrappers != null) {
                    valueContainer.addAll(suggestionWrappers);
                }

                if (resourceObjectTypesWrapper != null) {
                    valueContainer.addAll(resourceObjectTypesWrapper);
                }

                return valueContainer;
            }
        };

        return new MultivalueContainerListDataProvider<>(ResourceObjectTypesPanel.this, Model.of(), containerModel);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> columns = new ArrayList<>();
        LoadableDetachableModel<PrismContainerDefinition<ResourceObjectTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceObjectTypeDefinitionType> load() {
                ComplexTypeDefinition resourceDef =
                        PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                return resourceDef.findContainerDefinition(
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            }
        };

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ItemPath.create(ResourceObjectTypeDefinitionType.F_DELINEATION, ResourceObjectTypeDelineationType.F_OBJECT_CLASS),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

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
                StatusInfo<ObjectTypesSuggestionType> suggestionTypeStatusInfo = suggestionByWrapper.get(object);

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
