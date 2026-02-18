/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.CompareContainerPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.SuggestionsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

@PanelType(name = "resourceObjectTypes")
@PanelInstance(identifier = "resourceObjectTypes", applicableForType = ResourceType.class,
        childOf = SchemaHandlingPanel.class,
        display = @PanelDisplay(label = "PageResource.tab.objectTypes", icon = GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, order = 10))
public class ResourceObjectTypesPanel extends SchemaHandlingObjectsPanel<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypesPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected SuggestionsStorage.SuggestionType getSuggestionType() {
        return SuggestionsStorage.SuggestionType.OBJECT_TYPE;
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

        return columns;
    }

    @Override
    protected Class<ResourceObjectTypeDefinitionType> getSchemaHandlingObjectsType() {
        return ResourceObjectTypeDefinitionType.class;
    }

    @Override
    protected void onNewValue(
            PrismContainerValue<ResourceObjectTypeDefinitionType> value,
            @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target,
            boolean isDuplicate,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        objectDetailsModels.getPageResource().showObjectTypeWizard(value, target, newWrapperModel.getObject().getPath(), postSaveHandler);
    }

    @Override
    protected void onSuggestValue(
            IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target) {
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
    protected StatusAwareDataFactory.SuggestionsModelDto<ResourceObjectTypeDefinitionType> getSuggestionsModelDto() {
        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
        return StatusAwareDataFactory.createObjectTypeModel(
                this,
                getSwitchSuggestionModel(),
                resourceDefWrapper,
                getObjectWrapperObject().getOid());
    }

    @Override
    protected void onReviewValue(
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            AjaxRequestTarget target,
            StatusInfo<?> statusInfo,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {

        IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel = createContainerModel();

        PrismContainerValue<ResourceObjectTypeDefinitionType> original =
                valueModel.getObject().getNewValue().clone();
        WebPrismUtil.cleanupEmptyContainerValue(original);

        PrismContainerValue<ResourceObjectTypeDefinitionType> suggestion =
                processSuggestedContainerValue(original);

        onNewValue(suggestion, containerModel, target, false, postSaveHandler);
    }

    @Override
    protected boolean isStatisticsAllowed() {
        return true;
    }

    @Override
    protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
        super.customizeInlineMenuItems(inlineMenuItems);
        inlineMenuItems.add(createCompareWithExistingItemMenu());
    }

    private @NotNull InlineMenuItem createCompareWithExistingItemMenu() {
        return new InlineMenuItem(createStringResource("SmartSuggestObjectTypeTilePanel.compare.with.existing")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                List<ItemPath> requiredPaths = getDefaultObjectTypeComparePaths();

                return new ColumnMenuAction<>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (!(rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper)) {
                            return;
                        }

                        var selectedDef = (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>) wrapper;
                        ResourceObjectTypeDefinitionType resourceDef = selectedDef.getRealValue();
                        if (resourceDef == null
                                || resourceDef.getDelineation() == null
                                || resourceDef.getDelineation().getObjectClass() == null) {
                            warn(getString("ResourceObjectTypesPanel.compare.objectClass.no.suitable"));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        QName objectClass = resourceDef.getDelineation().getObjectClass();
                        var existingObjectClassDefs = getExistingObjectTypeDefinitions(objectClass);
                        var compareObjectDto = createCompareObjectDto(selectedDef, existingObjectClassDefs, requiredPaths);

                        var comparePanel = new CompareContainerPanel<>(getPageBase().getMainPopupBodyId(), () -> compareObjectDto);
                        getPageBase().showMainPopup(comparePanel, target);
                    }
                };
            }
        };
    }

    /**
     * Returns all existing object type definitions matching the given object class.
     */
    protected @NotNull List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getExistingObjectTypeDefinitions(
            @NotNull QName targetObjectClass) {

        PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> resourceWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        return resourceWrapper.getObject().getValues().stream()
                .filter(value -> {
                    ResourceObjectTypeDefinitionType def = value.getRealValue();
                    return def != null
                            && def.getDelineation() != null
                            && targetObjectClass.equals(def.getDelineation().getObjectClass());
                })
                .toList();
    }
}
