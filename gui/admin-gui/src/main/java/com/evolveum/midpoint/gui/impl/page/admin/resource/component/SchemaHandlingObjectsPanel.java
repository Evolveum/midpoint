/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
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
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

public abstract class SchemaHandlingObjectsPanel<C extends Containerable> extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

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

        WebMarkupContainer objectTypesPanel = createMultiValueListPanel();
        objectTypesPanel.setOutputMarkupId(true);
        form.add(objectTypesPanel);
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

        inlineMenuItems.add(createSuggestionOperationInlineMenu());
        inlineMenuItems.add(createSuggestionDetailsInlineMenu());
        inlineMenuItems.add(createSuggestionReviewInlineMenu());
        inlineMenuItems.add(createDeleteSuggestionInlineMenu());
    }

    private @NotNull MultivalueContainerListPanel<C> createMultiValueListPanel() {
        return new MultivalueContainerListPanel<C>(ID_TABLE, getSchemaHandlingObjectsType()) {
            @Override
            protected boolean isCreateNewObjectVisible() {
                return SchemaHandlingObjectsPanel.this.isCreateNewObjectVisible();
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return "table-td-middle";
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
                OperationResultStatusType status = statusFor(model.getObject());
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
                return createCustomActionsColumn(getPageBase(), inlineMenuItems);
            }

            private @Nullable IColumn<PrismContainerValueWrapper<C>, String> createCustomActionsColumn(
                    @NotNull PageBase pageBase,
                    @NotNull List<InlineMenuItem> allItems) {
                return !allItems.isEmpty() ? new InlineMenuButtonColumn<>(allItems, pageBase) {
                    @Override
                    public String getCssClass() {
                        return "inline-menu-column";
                    }

                    @Override
                    protected String getDropDownButtonIcon() {
                        return "fa fa-ellipsis-h";
                    }

                    @Override
                    protected String getSpecialButtonClass() {
                        return "btn btn-link btn-sm";
                    }

                    @Override
                    protected String getInlineMenuItemCssClass(IModel<PrismContainerValueWrapper<C>> rowModel) {
                        return "btn btn-link btn-sm text-nowrap";
                    }

                    @Override
                    protected String getAdditionalMultiButtonPanelCssClass() {
                        return "justify-content-end";
                    }
                } : null;
            }

            @Override
            public @NotNull List<InlineMenuItem> getInlineMenuItems() {
                List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
                customizeInlineMenuItems(inlineMenuItems);
                return inlineMenuItems;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> actions = new ArrayList<>();
                actions.add(createDeleteInlineMenu());
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
                createSuggestObjectButton(idButton, bar);
                createToggleSuggestionButton(idButton, bar);
                return bar;
            }

            @Override
            public boolean displayNoValuePanel() {
                return allowNoValuePanel() && hasNoValues();
            }

            private void createToggleSuggestionButton(String idButton, @NotNull List<Component> bar) {
                ToggleCheckBoxPanel togglePanel = new ToggleCheckBoxPanel(idButton,
                        switchSuggestion) {
                    @Override
                    public @NotNull Component getTitleComponent(String id) {
                        Label label = new Label(id, createStringResource("SchemaHandlingObjectsPanel.show.suggestion.label"));
                        label.add(AttributeAppender.append("class", "m-0 font-weight-normal text-body"));
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected void onToggle(@NotNull AjaxRequestTarget target) {
                        refreshForm(target);
                        target.add(SchemaHandlingObjectsPanel.this);
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String getContainerCssClass() {
                        return "d-flex flex-row-reverse align-items-center gap-1";
                    }
                };
                togglePanel.setOutputMarkupId(true);
                togglePanel.add(new VisibleBehaviour(() -> isToggleSuggestionVisible()));
                bar.add(togglePanel);
            }

            private void createSuggestObjectButton(String idButton, @NotNull List<Component> bar) {
                AjaxIconButton suggestObjectButton = new AjaxIconButton(idButton,
                        Model.of("fa-solid fa-wand-magic-sparkles"),
                        createStringResource("SchemaHandlingObjectsPanel.suggestNew")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        showSuggestConfirmDialog(getPageBase(),
                                createStringResource("SchemaHandlingObjectsPanel.suggestNew"), target);
                    }
                };
                suggestObjectButton.showTitleAsLabel(true);
                suggestObjectButton.add(AttributeAppender.replace("class", "btn btn-default btn-sm mr-2"));
                suggestObjectButton.add(new VisibleBehaviour(() -> isSuggestButtonVisible()));
                bar.add(suggestObjectButton);
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
            protected String getIconForNewObjectButton() {
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

                Task task = getPageBase().getPageTask();
                OperationResult result = task.getResult();

                toDelete.forEach(value -> {
                    if (deleteObjectTypeSuggestionIfRequested(value, task, result)) {return;}

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

            @SuppressWarnings("unchecked")
            private boolean deleteObjectTypeSuggestionIfRequested(
                    PrismContainerValueWrapper<C> value,
                    Task task,
                    OperationResult result) {
                StatusInfo<?> statusInfo = getStatusInfo(value);
                if (statusInfo != null && value.getRealValue() instanceof ResourceObjectTypeDefinitionType defToDelete) {
                    SmartIntegrationUtils.removeObjectTypeSuggestionNew(
                            getPageBase(),
                            (StatusInfo<ObjectTypesSuggestionType>) statusInfo,
                            defToDelete,
                            task,
                            result);
                    return true;
                }
                return false;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new PrismPropertyWrapperColumn<>(getContainerModel(), getPathForDisplayName(), AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<C>> rowModel) {

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
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel, List<PrismContainerValueWrapper<C>> listItems) {
                AbstractPageObjectDetails<?, ?> parent = findParent(AbstractPageObjectDetails.class);

                if (parent == null) {
                    getParentPage().warn("SchemaHandlingObjectsPanel.message.couldnOpenWizard");
                    return;
                }
                if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                    IModel<PrismContainerValueWrapper<C>> valueModel;
                    if (rowModel == null) {
                        valueModel = () -> listItems.iterator().next();
                    } else {
                        valueModel = rowModel;
                    }
                    onEditValue(valueModel, target);
                } else {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }

            @Override
            protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target,
                    AssignmentObjectRelation relationSpec, boolean isDuplicate) {
                onNewValue(value, getContainerModel(), target, isDuplicate);
            }

        };
    }

    protected ISelectableDataProvider<PrismContainerValueWrapper<C>> createProvider() {
        return null;
    }

    /**
     * This method is called when the name column is populated.
     * It allows for custom behavior or additional components to be added
     * to the name column item.
     *
     * @param cellItem The item being populated.
     * @param componentId The ID of the component being populated.
     * @param rowModel The model for the row being populated.
     * @return A component to be added to the cell, or null if no custom component is needed.
     */
    protected Component onNameColumnPopulateItem(
            Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem,
            String componentId,
            @NotNull IModel<PrismContainerValueWrapper<C>> rowModel) {
        PrismContainerValueWrapper<C> object = rowModel.getObject();
        StatusInfo<?> suggestionTypeStatusInfo = getStatusInfo(object);

        C realValue = object.getRealValue();
        if (suggestionTypeStatusInfo != null) {
            OperationResultStatusType status = suggestionTypeStatusInfo.getStatus();

            LoadableModel<String> displayNameModel = new LoadableModel<>() {
                @Override
                protected String load() {
                    if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
                        return createStringResource("ResourceObjectTypesPanel.suggestion.inProgress").getString();
                    }

                    if (realValue != null) {
                        if (realValue instanceof ResourceObjectTypeDefinitionType value) {
                            return value.getDisplayName();
                        }
                        if (realValue instanceof ShadowAssociationTypeDefinitionType value && value.getName() != null) {
                            return value.getName().getLocalPart();
                        }
                    }
                    return " - ";
                }
            };

            LabelWithBadgePanel labelWithBadgePanel = new LabelWithBadgePanel(
                    componentId, getAiBadgeModel(), displayNameModel) {
                @Override
                protected boolean isIconVisible() {
                    return status.equals(OperationResultStatusType.IN_PROGRESS);
                }

                @Contract(pure = true)
                @Override
                protected @NotNull String getIconCss() {
                    return GuiStyleConstants.ICON_FA_SPINNER + " fa-spin  text-info";
                }

                @Contract(pure = true)
                @Override
                protected @Nullable String getLabelCss() {
                    return status.equals(OperationResultStatusType.IN_PROGRESS)
                            ? " text-info"
                            : null;
                }

                @Override
                protected boolean isBadgeVisible() {
                    return status.equals(OperationResultStatusType.SUCCESS);
                }
            };
            labelWithBadgePanel.setOutputMarkupId(true);
            cellItem.add(labelWithBadgePanel);
            return labelWithBadgePanel;
        }
        return null;
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

    private InlineMenuItem createShowLifecycleStatesInlineMenu() {
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
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target, boolean isDuplicate);

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
     * <p>
     * This method inspects the {@link PrismObjectWrapper} associated with the current resource
     * and attempts to retrieve the {@link PrismContainerWrapper} at the path defined by
     * {@link #getTypesContainerPath()}. If the container does not exist or contains no values,
     * the method returns {@code true}.
     * </p>
     *
     * <p>This is typically used to decide whether to display the "no values" fallback panel.</p>
     *
     * @return {@code true} if the container is null or contains no values; {@code false} otherwise.
     * @throws IllegalStateException if the container cannot be found due to schema issues.
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

    protected void showSuggestConfirmDialog(
            @NotNull PageBase pageBase,
            StringResourceModel confirmModel, AjaxRequestTarget target) {
        SmartSuggestConfirmationPanel dialog = new SmartSuggestConfirmationPanel(pageBase.getMainPopupBodyId(), confirmModel) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                onSuggestValue(
                        null, createContainerModel(), target);
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    protected boolean isSuggestButtonVisible() {
        return true;
    }

    private void addAjaxTimeBehaviorIfRequested(
            PrismContainerValueWrapper<?> value,
            Item<PrismContainerValueWrapper<C>> item) {
        @Nullable StatusInfo<?> statusInfo = getStatusInfo(value);
        if (statusInfo != null && statusInfo.getStatus() != null) {
            item.add(AttributeModifier.append("class", SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).rowClass));

            boolean executing = statusInfo.isExecuting() && !statusInfo.isSuspended();
            if (executing) {
                AbstractAjaxTimerBehavior timer = new AbstractAjaxTimerBehavior(Duration.ofSeconds(3)) {
                    @Override
                    protected void onTimer(@NotNull AjaxRequestTarget target) {
                        target.add(item);
                        StatusInfo<?> statusInfo = getStatusInfo(value);
                        if (statusInfo == null || !statusInfo.isExecuting() || statusInfo.isSuspended()) {
                            stop(target);
                            refreshForm(target);
                            target.add(item);
                        }
                    }
                };
                item.add(timer);
            }
        }
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
        return isSuggestButtonVisible() && !hasNoValues();
    }

    protected ButtonInlineMenuItem createSuggestionDetailsInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.details.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<?> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.FATAL_ERROR;
                    }
                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                            StatusInfo<?> statusInfo = getStatusInfo(valueWrapper);
                            if (statusInfo == null) {
                                return;
                            }

                            HelpInfoPanel helpInfoPanel = new HelpInfoPanel(
                                    getPageBase().getMainPopupBodyId(),
                                    statusInfo::getLocalizedMessage) {
                                @Override
                                public StringResourceModel getTitle() {
                                    return createStringResource("ResourceObjectTypesPanel.suggestion.details.title");
                                }

                                @Override
                                protected @NotNull Label initLabel(IModel<String> messageModel) {
                                    Label label = super.initLabel(messageModel);
                                    label.add(AttributeModifier.append("class", "alert alert-danger"));
                                    return label;
                                }

                                @Override
                                public @NotNull Component getFooter() {
                                    Component footer = super.getFooter();
                                    footer.add(new VisibleBehaviour(() -> false));
                                    return footer;
                                }
                            };

                            target.add(getPageBase().getMainPopup());

                            getPageBase().showMainPopup(
                                    helpInfoPanel, target);
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createSuggestionOperationInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.suspend.generating.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getLabel() {
                ColumnMenuAction<?> action = (ColumnMenuAction<?>) getAction();
                IModel<?> rowModel = action.getRowModel();
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                    StatusInfo<?> suggestionStatus = getStatusInfo(wrapper);
                    if (suggestionStatus != null && suggestionStatus.isSuspended()) {
                        return createStringResource("ResourceObjectTypesPanel.resume.generating.inlineMenu");
                    }
                }
                return super.getLabel();
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                        StatusInfo<?> suggestionStatus = getStatusInfo(wrapper);
                        if (suggestionStatus == null) {
                            return false;
                        }
                        OperationResultStatusType status = suggestionStatus.getStatus();
                        return !suggestionStatus.isComplete() && status != OperationResultStatusType.FATAL_ERROR;
                    }

                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().getPageTask();
                        OperationResult result = task.getResult();

                        IModel<Serializable> rowModel = getRowModel();
                        if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> wrapper) {
                            StatusInfo<?> statusInfo = getStatusInfo(wrapper);
                            if (statusInfo != null) {
                                if (statusInfo.isSuspended() && statusInfo.getStatus() != OperationResultStatusType.FATAL_ERROR) {
                                    resumeSuggestionTask(getPageBase(), statusInfo, task, result);
                                } else {
                                    suspendSuggestionTask(getPageBase(), statusInfo, task, result);
                                }
                                refreshForm(target);
                            }
                        }
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createSuggestionReviewInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("ResourceObjectTypesPanel.review.suggestion.inlineMenu")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        StatusInfo<?> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null && suggestionStatus.getStatus() == OperationResultStatusType.SUCCESS;
                    }
                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        if (!(rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper)) {
                            getPageBase().warn("Couldn't get value from the row model.");
                            target.add(getPageBase().getFeedbackPanel().getParent());
                            return;
                        }

                        performOnReview(target, valueWrapper);
                    }
                };
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }
        };
    }

    protected ButtonInlineMenuItem createDeleteSuggestionInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    if (rowModel.getObject() instanceof PrismContainerValueWrapper<?> valueWrapper) {
                        @Nullable StatusInfo<?> suggestionStatus = getStatusInfo(valueWrapper);
                        return suggestionStatus != null;
                    }
                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<Serializable> rowModel = getRowModel();
                        performOnDeleteSuggestion(target, rowModel);
                    }
                };
            }
        };
    }

    protected void performOnDeleteSuggestion(AjaxRequestTarget target, IModel<Serializable> rowModel) {
    }

    protected void performOnReview(@NotNull AjaxRequestTarget target, @NotNull PrismContainerValueWrapper<?> valueWrapper) {
    }
}
