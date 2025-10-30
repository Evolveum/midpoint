/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.StatusAwareContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto.initDummyObjectTypePermissionData;

public abstract class SchemaHandlingObjectsPanel<C extends Containerable> extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final String ID_AI_PANEL = "aiPanel";
    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";

    private final IModel<Boolean> switchSuggestion = Model.of(Boolean.TRUE);

    public SchemaHandlingObjectsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        SmartAlertGeneratingPanel smartAlertGeneratingPanel = createSmartAlertGeneratingPanel();
        form.add(smartAlertGeneratingPanel);

        WebMarkupContainer objectTypesPanel = createMultiValueListPanel();
        objectTypesPanel.setOutputMarkupId(true);
        form.add(objectTypesPanel);
    }

    private @NotNull SmartAlertGeneratingPanel createSmartAlertGeneratingPanel() {
        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(ID_AI_PANEL,
                () -> new SmartGeneratingAlertDto(null, Model.of(), getPageBase())) {
            @Override
            protected void performSuggestOperation(AjaxRequestTarget target) {
                switchSuggestion.setObject(Boolean.TRUE);
                onSuggestValue(createContainerModel(), target);
            }

            @Override
            protected @NotNull IModel<RequestDetailsRecordDto> getPermissionRecordDtoIModel() {
                return () -> new RequestDetailsRecordDto(null, initDummyObjectTypePermissionData());
            }

            @Override
            protected void refreshAssociatedComponents(@NotNull AjaxRequestTarget target) {
                target.add(SchemaHandlingObjectsPanel.this);
            }
        };

        aiPanel.setOutputMarkupId(true);
        return aiPanel;
    }

    public <P extends Containerable> IModel<PrismContainerWrapper<P>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
    }

    private @NotNull StatusAwareContainerListPanel<C> createMultiValueListPanel() {
        return new StatusAwareContainerListPanel<C>(ID_TABLE, getSchemaHandlingObjectsType()) {

            @Override
            protected StatusAwareDataFactory.SuggestionsModelDto<C> getSuggestionsModelDto() {
                return SchemaHandlingObjectsPanel.this.getSuggestionsModelDto();
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return createContainerModel();
            }

            @Override
            public void refreshTable(AjaxRequestTarget target) {
                super.refreshTable(target);
                //TODO check it
                if(get(ID_FORM) != null){
                    target.add(get(ID_FORM));
                }
            }

            @Override
            protected IModel<Boolean> getSwitchSuggestion() {
                return switchSuggestion;
            }

            @Override
            protected ItemPath getPathForDisplayName() {
                return SchemaHandlingObjectsPanel.this.getPathForDisplayName();
            }

            @Override
            protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
                super.customizeInlineMenuItems(inlineMenuItems);
                SchemaHandlingObjectsPanel.this.customizeInlineMenuItems(inlineMenuItems);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return SchemaHandlingObjectsPanel.this.getTableId();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<C>, String>> columns = SchemaHandlingObjectsPanel.this.createColumns();

                LoadableDetachableModel<PrismContainerDefinition<C>> defModel = new LoadableDetachableModel<>() {
                    @Override
                    protected PrismContainerDefinition<C> load() {
                        ComplexTypeDefinition resourceDef = PrismContext.get().getSchemaRegistry()
                                .findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                        return resourceDef.findContainerDefinition(
                                ItemPath.create(getTypesContainerPath()));
                    }
                };

                columns.add(new LifecycleStateColumn<>(defModel, getPageBase()) {
                    @Override
                    public void populateItem(
                            Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem,
                            String componentId,
                            IModel<PrismContainerValueWrapper<C>> rowModel) {
                        OperationResultStatusType status = statusFor(rowModel.getObject());
                        if (status == null) {
                            super.populateItem(cellItem, componentId, rowModel);
                            return;
                        }
                        var style = SuggestionUiStyle.from(status);
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
            protected IColumn<PrismContainerValueWrapper<C>, String> createActionsColumn() {
                List<InlineMenuItem> inlineMenuItems = getInlineMenuItems();
                return createLinkStyleActionsColumn(getPageBase(), inlineMenuItems);
            }

            @Override
            public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
                if (isStatisticsAllowed()) {
                    inlineMenuItems.add(createStatisticsInlineMenu());
                }
                return inlineMenuItems;
            }

            @Override
            protected void performOnReview(
                    @NotNull AjaxRequestTarget target,
                    @NotNull PrismContainerValueWrapper<C> valueWrapper,
                    @NotNull StatusInfo<?> statusInfo) {
                PageBase pageBase = getPageBase();
                onReviewValue(() -> valueWrapper, target, statusInfo,
                        ajaxRequestTarget -> performOnDeleteSuggestion(pageBase, ajaxRequestTarget,
                                valueWrapper, statusInfo));
            }

            public @NotNull InlineMenuItem createStatisticsInlineMenu() {
                return new InlineMenuItem(createStringResource("Statistics.button.label")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new ColumnMenuAction<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                if (getRowModel() != null) {
                                    ResourceType resource = getObjectDetailsModels().getObjectType();
                                    String resourceOid = resource.getOid();

                                    var object = (PrismContainerValueWrapper<?>) getRowModel().getObject();
                                    if (object.getRealValue() instanceof ResourceObjectTypeDefinitionType objectDef) {
                                        showStatisticsPanel(target, objectDef, getPageBase(), resourceOid);
                                    }

                                }
                            }
                        };
                    }
                };
            }

            @Override
            protected String getKeyOfTitleForNewObjectButton() {
                return SchemaHandlingObjectsPanel.this.getKeyOfTitleForNewObjectButton();
            }

            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel,
                    List<PrismContainerValueWrapper<C>> listItems) {
                AbstractPageObjectDetails<?, ?> parent = findParent(AbstractPageObjectDetails.class);

                if (parent == null) {
                    getParentPage().warn("SchemaHandlingObjectsPanel.message.couldnOpenWizard");
                    return;
                }
                if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                    IModel<PrismContainerValueWrapper<C>> valueModel;
                    valueModel = Objects.requireNonNullElseGet(rowModel, () -> () -> listItems != null
                            ? listItems.iterator().next() : null);
                    onEditValue(valueModel, target);
                } else {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }

            @Override
            protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target,
                    AssignmentObjectRelation relationSpec, boolean isDuplicate) {
                onNewValue(value, getContainerModel(), target, isDuplicate, null);
            }

        };
    }

    protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
    }

    protected ItemPath getPathForDisplayName() {
        return ResourceObjectTypeDefinitionType.F_DISPLAY_NAME;
    }

    protected abstract ItemPath getTypesContainerPath();

    protected abstract UserProfileStorage.TableId getTableId();

    protected abstract String getKeyOfTitleForNewObjectButton();

    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns();

    protected abstract Class<C> getSchemaHandlingObjectsType();

    protected abstract StatusAwareDataFactory.SuggestionsModelDto<C> getSuggestionsModelDto();

    protected abstract void onNewValue(
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target,
            boolean isDuplicate, @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler);

    protected abstract void onSuggestValue(IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target);

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<C>> valueModel, AjaxRequestTarget target);

    protected abstract void onReviewValue(@NotNull IModel<PrismContainerValueWrapper<C>> valueModel, AjaxRequestTarget target,
            StatusInfo<?> statusInfo, @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler);

    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    public MultivalueContainerListPanel getTable() {
        Component component = get(getPageBase().createComponentPath(ID_FORM, ID_TABLE));
        if (component instanceof MultivalueContainerListPanel) {
            return (MultivalueContainerListPanel) component;
        } else {
            return null;
        }
    }

    /**
     * Checks whether the container at the specified path has any values.
     * If the container does not exist or has no values, returns true.
     */

    protected IModel<Boolean> getSwitchSuggestionModel() {
        return switchSuggestion;
    }

    protected boolean isStatisticsAllowed() {
        return false;
    }
}
