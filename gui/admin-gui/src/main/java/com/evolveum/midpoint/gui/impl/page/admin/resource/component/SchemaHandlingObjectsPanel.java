/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.LabelWithBadgePanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
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
                onSuggestValue(null, createContainerModel(), target);
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

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
    }

    protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
        for (InlineMenuItem menuItem : inlineMenuItems) {
            menuItem.setVisibilityChecker((InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper) {
                    return statusFor((PrismContainerValueWrapper<?>) rowModel.getObject()) == null;
                }
                return true;
            });
        }

        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshForm));
        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
        inlineMenuItems.add(createSuggestionReviewInlineMenu(getPageBase(), this::getStatusInfo, this::performOnReview));
    }

    private @NotNull MultivalueContainerListPanel<C> createMultiValueListPanel() {
        return new MultivalueContainerListPanel<C>(ID_TABLE, getSchemaHandlingObjectsType()) {
            @Override
            protected boolean isCreateNewObjectVisible() {
                return SchemaHandlingObjectsPanel.this.isCreateNewObjectVisible();
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return "card table-td-middle";
            }

            @Override
            protected String getAdditionalFooterCssClasses() {
                return "bg-white";
            }

            @Override
            protected ISelectableDataProvider<PrismContainerValueWrapper<C>> createProvider() {
                if (SchemaHandlingObjectsPanel.this.createProvider() != null) {
                    return SchemaHandlingObjectsPanel.this.createProvider();
                }
                return super.createProvider();
            }

            @Override
            protected void customProcessNewRowItem(Item<PrismContainerValueWrapper<C>> item, IModel<PrismContainerValueWrapper<C>> model) {
                StatusInfo<?> status = getStatusInfo(model.getObject());
                if (status == null) {
                    super.customProcessNewRowItem(item, model);
                    return;
                }
                item.add(AttributeModifier.replace("class", SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
                addAjaxTimeBehaviorIfRequested(model.getObject(), item);
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return createContainerModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return SchemaHandlingObjectsPanel.this.getTableId();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createDefaultColumns() {
                return SchemaHandlingObjectsPanel.this.createColumns();
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createActionsColumn() {
                List<InlineMenuItem> inlineMenuItems = getInlineMenuItems();
                return createLinkStyleActionsColumn(getPageBase(), inlineMenuItems);
            }

            @Override
            public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
                customizeInlineMenuItems(inlineMenuItems);

                if (isStatisticsAllowed()) {
                    inlineMenuItems.add(createStatisticsInlineMenu());
                }
                inlineMenuItems.add(createDeleteInlineMenu());
                return inlineMenuItems;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> actions = new ArrayList<>();
                ButtonInlineMenuItem editInlineMenu = createEditInlineMenu();
                editInlineMenu.setLabelVisible(true);
                actions.add(editInlineMenu);
                actions.add(createShowLifecycleStatesInlineMenu());
                return actions;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String idButton) {
                List<Component> bar = new ArrayList<>();
                createNewObjectPerformButton(idButton, bar);
                ToggleCheckBoxPanel togglePanel = createToggleSuggestionVisibilityButton(getPageBase(),idButton, switchSuggestion,
                        (target) -> refreshForm(target), SchemaHandlingObjectsPanel.this);
                togglePanel.add(new VisibleBehaviour(() -> isToggleSuggestionVisible()));
                bar.add(togglePanel);
                return bar;
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
            public boolean displayNoValuePanel() {
                return allowNoValuePanel() && hasNoValues();
            }

            private void createNewObjectPerformButton(String idButton, @NotNull List<Component> bar) {
                AjaxIconButton newObjectButton = new AjaxIconButton(idButton,
                        new Model<>(getIconForNewObjectButton()),
                        createStringResource(getKeyOfTitleForNewObjectButton())) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newItemPerformed(target, null);
                    }
                };
                newObjectButton.showTitleAsLabel(true);
                newObjectButton.add(AttributeAppender.replace("class", "btn btn-primary btn-sm mr-2"));
                newObjectButton.add(new VisibleBehaviour(this::isCreateNewObjectVisible));
                bar.add(newObjectButton);
            }

            @Override
            protected @NotNull String getIconForNewObjectButton() {
                return "fa fa-circle-plus";
            }

            @Override
            protected String getKeyOfTitleForNewObjectButton() {
                return SchemaHandlingObjectsPanel.this.getKeyOfTitleForNewObjectButton();
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected boolean allowEditMultipleValuesAtOnce() {
                return false;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createCheckboxColumn() {
                return new CheckBoxHeaderColumn<>();
            }

            public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<C>> toDelete) {
                if (toDelete == null || toDelete.isEmpty()) {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                toDelete.forEach(value -> {
                    var statusInfo = getStatusInfo(value);
                    if (performOnDeleteSuggestion(getPageBase(), target, value, statusInfo)) {
                        return;
                    }

                    if (value.getStatus() == ValueStatus.ADDED) {
                        PrismContainerWrapper<C> wrapper = getContainerModel() != null ?
                                getContainerModel().getObject() : null;
                        if (wrapper != null) {
                            wrapper.getValues().remove(value);
                        }
                    } else {
                        value.setStatus(ValueStatus.DELETED);
                    }
                    value.setSelected(false);
                });
                refreshForm(target);
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createNameColumn(IModel<String> displayModel,
                    GuiObjectColumnType customColumn,
                    ExpressionType expression) {
                return new PrismPropertyWrapperColumn<>(getContainerModel(), getPathForDisplayName(),
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId,
                            IModel<PrismContainerValueWrapper<C>> rowModel) {

                        Component component = onNameColumnPopulateItem(cellItem, componentId, rowModel);
                        if (component != null) {
                            cellItem.add(component);
                            return;
                        }

                        super.populateItem(cellItem, componentId, rowModel);
                    }

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {
                        editItemPerformed(target, model, null);
                    }
                };
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
                    valueModel = Objects.requireNonNullElseGet(rowModel, () -> () -> listItems.iterator().next());
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

    protected ISelectableDataProvider<PrismContainerValueWrapper<C>> createProvider() {
        return null;
    }

    /**
     * This method is called when the name column is populated.
     * It allows for custom rendering of the name column, e.g. to show status badges.
     */
    protected Component onNameColumnPopulateItem(
            Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem,
            String componentId,
            @NotNull IModel<PrismContainerValueWrapper<C>> rowModel) {

        PrismContainerValueWrapper<C> wrapper = rowModel.getObject();
        StatusInfo<?> statusInfo = getStatusInfo(wrapper);
        if (statusInfo == null) {
            return null;
        }

        OperationResultStatusType status = statusInfo.getStatus();
        C realValue = wrapper.getRealValue();

        LoadableModel<String> displayNameModel = new LoadableModel<>() {
            @Override
            protected String load() {
                if (status == OperationResultStatusType.IN_PROGRESS || status == OperationResultStatusType.UNKNOWN) {
                    return createStringResource("Generating.suggestion").getString();
                }

                if (realValue instanceof ResourceObjectTypeDefinitionType typeDef) {
                    return typeDef.getDisplayName();
                }

                if (realValue instanceof ShadowAssociationTypeDefinitionType assocDef) {
                    return assocDef.getName() != null ? assocDef.getName().getLocalPart() : " - ";
                }

                return " - ";
            }
        };

        LabelWithBadgePanel label = buildSuggestionNameLabel(componentId, statusInfo, displayNameModel, status);
        cellItem.add(label);
        return label;
    }

    @Nullable
    protected StatusAwareDataProvider<?> getStatusAwareProvider() {
        var dp = getTable().getDataProvider();
        return (dp instanceof StatusAwareDataProvider<?> sap)
                ? sap
                : null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected @Nullable StatusInfo<?> getStatusInfo(PrismContainerValueWrapper<?> value) {
        var provider = getStatusAwareProvider();
        return provider != null ? provider
                .getSuggestionInfo((PrismContainerValueWrapper) value) : null;
    }

    @Nullable
    protected OperationResultStatusType statusFor(
            PrismContainerValueWrapper<?> wrapper) {
        StatusInfo<?> info = getStatusInfo(wrapper);
        return info != null ? info.getStatus() : null;
    }

    private @NotNull InlineMenuItem createShowLifecycleStatesInlineMenu() {
        return new InlineMenuItem(createStringResource("SchemaHandlingObjectsPanel.button.showLifecycleStates")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<C>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        OnePanelPopupPanel popupPanel = new OnePanelPopupPanel(
                                getPageBase().getMainPopupBodyId(),
                                1100,
                                600,
                                createStringResource("ContainerWithLifecyclePanel.popup.title")) {
                            @Override
                            protected WebMarkupContainer createPanel(String id) {
                                return new ContainerWithLifecyclePanel<>(id, getRowModel());
                            }

                            @Override
                            protected void processHide(AjaxRequestTarget target) {
                                super.processHide(target);
                                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, getRowModel().getObject());
                            }
                        };

                        getPageBase().showMainPopup(popupPanel, target);

                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
    }

    protected ItemPath getPathForDisplayName() {
        return ResourceObjectTypeDefinitionType.F_DISPLAY_NAME;
    }

    protected abstract ItemPath getTypesContainerPath();

    protected abstract UserProfileStorage.TableId getTableId();

    protected abstract String getKeyOfTitleForNewObjectButton();

    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns();

    protected abstract Class<C> getSchemaHandlingObjectsType();

    protected abstract void onNewValue(
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target,
            boolean isDuplicate, @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler);

    protected abstract void onSuggestValue(
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target);

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<C>> valueModel, AjaxRequestTarget target);

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
     * Determines whether the panel should display a special UI component
     * (e.g. {@link NoValuePanel}) when there are no values
     * present in the container.
     */
    protected boolean allowNoValuePanel() {
        return false;
    }

    /**
     * Checks whether the container at the specified path has any values.
     * If the container does not exist or has no values, returns true.
     */
    protected boolean hasNoValues() {
        PrismObjectWrapper<ResourceType> object = getObjectWrapperModel().getObject();
        if (object == null) {
            return true;
        }

        PrismContainerWrapper<C> typesContainer;
        try {
            typesContainer = object.findContainer(getTypesContainerPath());
        } catch (SchemaException e) {
            throw new IllegalStateException("Cannot find container for path: " + getTypesContainerPath(), e);
        }
        if (typesContainer == null) {
            return true;
        }

        return typesContainer.getValues() == null || typesContainer.getValues().isEmpty();
    }

    private void addAjaxTimeBehaviorIfRequested(
            PrismContainerValueWrapper<C> value,
            Item<PrismContainerValueWrapper<C>> item) {
        applySuggestionAutoRefresh(value, item, Duration.ofSeconds(3), this::getStatusInfo, this::refreshForm);
    }

    protected IModel<Boolean> getSwitchSuggestionModel() {
        return switchSuggestion;
    }

    protected void refreshForm(@NotNull AjaxRequestTarget target) {
        if (getTable().getDataProvider() instanceof MultivalueContainerListDataProvider<?> provider) {
            provider.getModel().detach();
        }
        getTable().refreshTable(target);
        target.add(get(ID_FORM));
    }

    protected boolean isToggleSuggestionVisible() {
        return !hasNoValues();
    }

    protected boolean performOnDeleteSuggestion(
            @NotNull PageBase pageBase,
            AjaxRequestTarget target,
            PrismContainerValueWrapper<C> rowModel,
            @Nullable StatusInfo<?> statusInfo) {
        return false;
    }

    protected void performOnReview(@NotNull AjaxRequestTarget target, @NotNull PrismContainerValueWrapper<?> valueWrapper) {
    }

    protected boolean isStatisticsAllowed() {
        return false;
    }
}
