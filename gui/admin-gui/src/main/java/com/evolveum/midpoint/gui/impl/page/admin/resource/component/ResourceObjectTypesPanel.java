/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectTypeSuggestionWrappers;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

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
                getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel) {
                StatusInfo<ObjectTypesSuggestionType> statusInfo = getStatusInfo(rowModel.getObject());
                if (statusInfo != null) {
                    QName objectClassName = statusInfo.getObjectClassName();
                    if (objectClassName != null) {
                        Label label = new Label(componentId, objectClassName.getLocalPart());
                        label.setOutputMarkupId(true);
                        cellItem.add(label);
                        return;
                    }
                }
                super.populateItem(cellItem, componentId, rowModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_KIND,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ResourceObjectTypeDefinitionType.F_INTENT,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

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
                OperationResultStatusType status = statusFor(rowModel.getObject());
                if (status == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                    return;
                }
                var style = SmartIntegrationUtils.SuggestionUiStyle.from(status);
                Label statusLabel = new Label(componentId, createStringResource(
                        "ResourceObjectTypesPanel.suggestion." + status.value()));
                statusLabel.setOutputMarkupId(true);
                statusLabel.add(AttributeModifier.append("class", style.badgeClass));
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
            PrismContainerValue<ResourceObjectTypeDefinitionType> value,
            IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target,
            boolean isDuplicate,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showObjectTypeWizard(value, target, newWrapperModel.getObject().getPath(), postSaveHandler);
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

    protected @Nullable StatusInfo<ObjectTypesSuggestionType> getStatusInfo(PrismContainerValueWrapper<?> value) {
        StatusInfo<?> statusInfo = super.getStatusInfo(value);
        if (statusInfo != null) {
            return (StatusInfo<ObjectTypesSuggestionType>) statusInfo;
        }
        return null;
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createProvider() {
        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
        var suggestionsModelDto = StatusAwareDataFactory.createObjectTypeModel(
                this,
                getSwitchSuggestionModel(),
                resourceDefWrapper,
                getObjectWrapperObject().getOid());

        return new StatusAwareDataProvider<>(this, Model.of(), suggestionsModelDto);
    }

    @Override
    protected boolean hasNoValues() {
        return Objects.requireNonNull(getStatusAwareProvider()).size() == 0;
    }

    @Override
    protected boolean isToggleSuggestionVisible() {
        final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        final OperationResult result = task.getResult();

        final String resourceOid = getObjectDetailsModels().getObjectType().getOid();

        SuggestionProviderResult<ResourceObjectTypeDefinitionType, ObjectTypesSuggestionType> suggestions = loadObjectTypeSuggestionWrappers(
                getPageBase(), resourceOid, task, result);

        return !suggestions.wrappers().isEmpty();
    }

    protected boolean performOnDeleteSuggestion(
            @NotNull PageBase pageBase,
            AjaxRequestTarget target,
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> valueWrapper,
            @Nullable StatusInfo<?> statusInfo) {
        Task task = pageBase.createSimpleTask(OP_DETERMINE_STATUSES);
        OperationResult result = task.getResult();
        if (statusInfo == null) {
            return false;
        }
        SmartIntegrationUtils.removeObjectTypeSuggestionNew(
                pageBase,
                statusInfo,
                valueWrapper.getRealValue(),
                task,
                result);
        target.add(pageBase.getFeedbackPanel());
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void performOnReview(@NotNull AjaxRequestTarget target, @NotNull PrismContainerValueWrapper<?> valueWrapper) {
        IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createContainerModel();
        PrismContainerValue<ResourceObjectTypeDefinitionType> originalObject = valueWrapper.getOldValue();
        WebPrismUtil.cleanupEmptyContainerValue(originalObject);

        PrismContainerValue<ResourceObjectTypeDefinitionType> suggestionToAdd = processSuggestedContainerValue(
                originalObject);

        PageBase pageBase = getPageBase();
        var statusInfo = getStatusInfo(valueWrapper);
        onNewValue(suggestionToAdd, containerModel, target, false,
                ajaxRequestTarget -> performOnDeleteSuggestion(pageBase, ajaxRequestTarget,
                        (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>) valueWrapper, statusInfo));
    }

    @Override
    protected boolean isStatisticsAllowed() {
        return true;
    }
}
