/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeSuggestionValue;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.acceptConfirmationTitle;

public abstract class AssociationTablePanel
        extends MultiSelectContainerActionTileTablePanel<
        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>,
        ShadowAssociationTypeDefinitionType,
        SmartAssociationTileModel> {

    public AssociationTablePanel(
            String id,
            UserProfileStorage.TableId tableId,
            IModel<ViewToggle> toggleView,
            IModel<Boolean> switchModel) {
        super(id, tableId, toggleView, switchModel);
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ShadowAssociationTypeDefinitionType.class;
    }

    @Override
    protected String getTileCssStyle() {
        return "min-height: 130px;";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12";
    }

    @Override
    protected String getTileContainerCssClass() {
        return super.getTileContainerCssClass() + "gap-3";
    }

    @Override
    protected IModel<String> getNewObjectButtonLabel() {
        return createStringResource("AssociationTypesPanel.newObject");
    }

    @Override
    protected void newItemPerformed(
            PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
            AjaxRequestTarget target,
            AssignmentObjectRelation relationSpec,
            boolean isDuplicate,
            StatusInfo<?> statusInfo) {
        newItemPerformedAction(value, target, relationSpec, isDuplicate, statusInfo);
    }

    @Override
    protected SmartAssociationTileModel createTileObject(
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> object) {

        return new SmartAssociationTileModel(
                object,
                getResourceType(),
                getStatusInfoToken(object));
    }

    @Override
    protected Component createTile(String id, IModel<SmartAssociationTileModel> model) {
        return new SmartAssociationTilePanel(id, model) {

            @Override
            public @NotNull Component createDetailsPanel(String id,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value, IModel<Boolean> isDetailedViewModel) {
                return AssociationTablePanel.this.createDetailsPanel(id, value, isDetailedViewModel);
            }

            @Override
            protected void performAcceptAction(@NotNull AjaxRequestTarget target,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value) {
                performAcceptOperationAction(target, value);
            }

            @Override
            protected void performDismissAction(@NotNull AjaxRequestTarget target,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value) {
                performOnDeleteSuggestion(getPageBase(), target, value, getStatusInfoObject(value));
                refreshAndDetach(target);
            }

            @Override
            protected void performEditAction(@NotNull AjaxRequestTarget target,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value) {
                performEditOperationAction(target, value);
            }
        };
    }

    @Override
    protected MultivalueContainerListDataProvider<ShadowAssociationTypeDefinitionType> createDataProvider() {
        var dto = getSuggestionsModelDto();
        return new StatusAwareDataProvider<>(this, Model.of(), dto, false);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String>> createDomainColumns() {
        return List.of();
    }

    @Override
    protected void deselectItem(PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> entry) {
        // no-op
    }

    @Override
    protected IModel<String> getItemLabelModel(PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> entry) {
        return null;
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> getSelectedItemsModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> load() {
                List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> all = getCurrentPageItems();
                return all == null ? List.of() : all.stream().filter(PrismContainerValueWrapper::isSelected).toList();
            }
        };
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> toolbarButtonsList = super.createToolbarButtonsList(idButton);
        toolbarButtonsList.add(createAcceptAllButton(idButton));
        toolbarButtonsList.add(createDiscardAllButton(idButton));
        return toolbarButtonsList;
    }

    @Override
    protected WebMarkupContainer createTablePanel(
            String idTable,
            ISortableDataProvider<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String> provider,
            UserProfileStorage.TableId tableId) {

        return new ContainerableListPanel<ShadowAssociationTypeDefinitionType, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>(
                idTable, ShadowAssociationTypeDefinitionType.class) {
            @Override
            protected UserProfileStorage.TableId getTableId() {
                return tableId;
            }

            @Override
            protected boolean isCollapsableTable() {
                return true;
            }

            @Override
            protected Component createCollapsibleContent(String id,
                    @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> rowModel) {
                return AssociationTablePanel.this.createDetailsPanel(id, rowModel.getObject(), Model.of(true));
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String> createIconColumn() {
                return null;
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return "table-td-middle m-0";
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String>> columns = new ArrayList<>();
                LoadableDetachableModel<PrismContainerDefinition<ShadowAssociationTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
                    @Override
                    protected PrismContainerDefinition<ShadowAssociationTypeDefinitionType> load() {
                        ComplexTypeDefinition resourceDef =
                                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                        return resourceDef.findContainerDefinition(
                                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_ASSOCIATION_TYPE));
                    }
                };

                columns.add(new AbstractColumn<>(createStringResource("AssociationTypesPanel.column.name")) {
                    @Override
                    public void populateItem(
                            Item<ICellPopulator<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> item,
                            String s,
                            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> iModel) {
                        var valueWrapper = iModel.getObject();
                        if (getStatusInfoObject(valueWrapper) != null) {
                            RepeatingView container = new RepeatingView(s);
                            Label nameLabel = new Label(container.newChildId(), valueWrapper.getRealValue().getName() != null
                                    ? valueWrapper.getRealValue().getName() : "-");
                            nameLabel.add(AttributeModifier.append("class", "font-weight-semibold"));

                            IconWithLabel tag = buildTag(container.newChildId());
                            container.add(nameLabel);
                            container.add(tag);

                            item.add(container);
                        } else {
                            Label nameLabel = new Label(s, valueWrapper.getRealValue().getName() != null
                                    ? valueWrapper.getRealValue().getName()
                                    : "-");
                            nameLabel.add(AttributeModifier.append("class", "font-weight-semibold"));
                            item.add(nameLabel);
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-3 text-left";
                    }
                });

                columns.add(new PrismPropertyWrapperColumn<>(
                        defModel,
                        ShadowAssociationTypeDefinitionType.F_DISPLAY_NAME,
                        AbstractItemWrapperColumn.ColumnType.STRING,
                        getPageBase()));

                columns.add(new LifecycleStateColumn<>(defModel, getPageBase()) {
                    @Override
                    public void populateItem(
                            Item<ICellPopulator<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> cellItem,
                            String componentId,
                            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> rowModel) {

                        StatusInfo<?> statusInfo = getStatusInfoObject(rowModel.getObject());
                        OperationResultStatusType status = statusInfo != null ? statusInfo.getStatus() : null;

                        if (status == null) {
                            super.populateItem(cellItem, componentId, rowModel);
                            return;
                        }

                        SmartIntegrationUtils.SuggestionUiStyle style = SmartIntegrationUtils.SuggestionUiStyle.from(status);

                        Label statusLabel = new Label(componentId,
                                createStringResource("ResourceObjectTypesPanel.suggestion." + status.value()));

                        statusLabel.setOutputMarkupId(true);
                        statusLabel.add(AttributeModifier.append("class", style.badgeClass));

                        cellItem.add(statusLabel);
                    }
                });

                columns.add(new AbstractColumn<>(createStringResource("")) {
                    @Override
                    public void populateItem(
                            Item<ICellPopulator<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> item,
                            String s,
                            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> iModel) {
                        var valueWrapper = iModel.getObject();
                        if (getStatusInfoObject(valueWrapper) != null) {
                            RepeatingView container = new RepeatingView(s);

                            AjaxIconButton acceptButton = buildSuggestionAcceptButton(container.newChildId(), valueWrapper);
                            container.add(acceptButton);

                            AjaxIconButton dismissButton = buildSuggestionDismissButton(container.newChildId(), valueWrapper);
                            container.add(dismissButton);
                            item.add(container);
                        } else {
                            AjaxIconButton editButton = buildEditButton(s, valueWrapper);
                            item.add(editButton);
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-2 text-right";
                    }
                });

                return columns;
            }

            @Override
            protected Component createHeader(String headerId) {
                Fragment f = createHeaderFragment(headerId);
                f.add(AttributeModifier.replace("class", "p-3"));
                return f;
            }

            @Override
            protected void customProcessNewRowItem(
                    Item<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> item,
                    IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> model) {

                if (getStatusInfoObject(model.getObject()) != null) {
                    item.add(AttributeModifier.append("class", "table-system"));
                }
            }

            @Override
            protected ISelectableDataProvider<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> createProvider() {
                return (ISelectableDataProvider<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>) provider;
            }

            @Override
            public List<ShadowAssociationTypeDefinitionType> getSelectedRealObjects() {
                return List.of();
            }
        };
    }

    public @NotNull Component createDetailsPanel(String id,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value,
            @NotNull IModel<Boolean> isDetailedView) {

        AssociationDetailsPanel associationDetailsPanel = new AssociationDetailsPanel(id, () -> value) {
            @Override
            protected boolean isSuggestion() {
                return getStatusInfoObject(value) != null;
            }

            @Override
            protected void processReviewButtonClick(@NotNull AjaxRequestTarget target) {
                onReviewValue(() -> value, target, getStatusInfoObject(value),
                        ajaxRequestTarget -> performOnDeleteSuggestion(getPageBase(), ajaxRequestTarget,
                                value, getStatusInfoObject(value)));
            }
        };
        associationDetailsPanel.setOutputMarkupId(true);
        associationDetailsPanel.add(new VisibleBehaviour(isDetailedView::getObject));
        return associationDetailsPanel;
    }

    protected void performOnDeleteSuggestion(
            @NotNull PageBase pageBase,
            AjaxRequestTarget target,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueWrapper,
            @Nullable StatusInfo<?> statusInfo) {
        Task task = pageBase.createSimpleTask("delete suggestion");
        OperationResult result = task.getResult();

        if (statusInfo != null) {
            removeSuggestionValue(pageBase, target, valueWrapper, statusInfo, task, result);
        }
    }

    public abstract void newItemPerformedAction(
            PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
            AjaxRequestTarget target,
            AssignmentObjectRelation relationSpec,
            boolean isDuplicate,
            StatusInfo<?> statusInfo);

    public abstract void performAcceptOperationAction(@NotNull AjaxRequestTarget target,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value);

    public abstract void performEditOperationAction(@NotNull AjaxRequestTarget target,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value);

    private @NotNull AjaxIconButton buildSuggestionAcceptButton(String id,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> object) {
        AjaxIconButton accept = new AjaxIconButton(id, () -> "fa fa-check",
                createStringResource("SmartAssociationTilePanel.accept")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performAcceptOperationAction(target, object);
                refreshAndDetach(target);
            }
        };

        accept.setOutputMarkupId(true);
        accept.showTitleAsLabel(true);
        accept.add(AttributeModifier.append("class", "btn btn-outline-primary me-2 gap-2"));
        return accept;
    }

    private @NotNull AjaxIconButton buildSuggestionDismissButton(String id,
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> object) {
        AjaxIconButton dismiss = new AjaxIconButton(id, () -> "fa fa-check",
                createStringResource("SmartAssociationTilePanel.dismiss")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performOnDeleteSuggestion(getPageBase(), target, object, getStatusInfoObject(object));
            }
        };

        dismiss.setOutputMarkupId(true);
        dismiss.showTitleAsLabel(true);
        dismiss.add(AttributeModifier.append("class", "btn btn-link text-danger me-2 gap-2"));
        return dismiss;
    }

    private @NotNull AjaxIconButton buildEditButton(String id, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> object) {
        AjaxIconButton edit = new AjaxIconButton(id, () -> "fa fa-edit",
                createStringResource("SmartAssociationTilePanel.edit")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                performEditOperationAction(target, object);
            }
        };

        edit.setOutputMarkupId(true);
        edit.showTitleAsLabel(true);
        edit.add(AttributeModifier.append("class", "btn btn-sm btn-outline-primary me-2 gap-2 text-nowrap"));
        return edit;
    }

    //TODO AI/SYSTEM
    private @NotNull IconWithLabel buildTag(@NotNull String id) {
        IconWithLabel tag = new IconWithLabel(id, () -> "System suggestion") {
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-gear mr-1";
            }
        };

        tag.setOutputMarkupId(true);
        tag.add(AttributeModifier.replace("class", "system-badge"));
        return tag;
    }

    protected AjaxIconButton createDiscardAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, "SmartMappingTable.discard.all",
                "btn-default text-purple", false);
    }

    protected AjaxIconButton createAcceptAllButton(String id) {
        return createAcceptDiscardBulkActionButton(id, "SmartMappingTable.apply.all",
                "btn-success bg-purple", true);
    }

    protected AjaxIconButton createAcceptDiscardBulkActionButton(String id, String labelKey, String cssClass, boolean isAccept) {
        AjaxIconButton button = new AjaxIconButton(id, Model.of(""), createStringResource(labelKey)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> allItems = getAllItems();
                if (!allItems.isEmpty()) {

                    ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                            acceptConfirmationTitle(getPageBase(), allItems.size(), getCurrentPageItems().isEmpty())) {

                        @Override
                        public void yesPerformed(AjaxRequestTarget target) {
                            allItems.stream()
                                    .filter(v -> getStatusInfoObject(v) != null)
                                    .forEach(v -> {
                                        if (isAccept) {
                                            performAcceptOperationAction(target, v);
                                        } else {
                                            performOnDeleteSuggestion(getPageBase(), target, v, getStatusInfoObject(v));
                                        }
                                    });
                            refreshAndDetach(target);
                        }
                    };

                    getPageBase().showMainPopup(dialog, target);
                }
            }
        };

        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        button.add(AttributeModifier.replace("class", "ml-2 px-2 btn " + cssClass));
        button.add(new VisibleBehaviour(() -> getSwitchToggleModel().getObject().equals(Boolean.TRUE)));
        return button;
    }

    protected abstract void onReviewValue(
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
            AjaxRequestTarget target,
            StatusInfo<?> statusInfo,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler);

    protected abstract StatusAwareDataFactory.SuggestionsModelDto<ShadowAssociationTypeDefinitionType> getSuggestionsModelDto();

    protected abstract ResourceType getResourceType();

    protected boolean isHeaderPanelHeaderVisible() {
        return false;
    }

}
