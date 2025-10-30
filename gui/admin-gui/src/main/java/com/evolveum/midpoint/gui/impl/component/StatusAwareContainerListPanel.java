package com.evolveum.midpoint.gui.impl.component;/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.LabelWithBadgePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ContainerWithLifecyclePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.NoValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectTypesPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.AttributeModifier;
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

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeSuggestionValue;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.*;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.createSuggestionReviewInlineMenu;

/**
 * An extension of {@link MultivalueContainerListPanel} that is aware of
 * {@link StatusInfo}-based suggestion states. It automatically applies
 * custom row styles and auto-refresh behavior for items that have status information.
 *
 * <p>Subclasses can override {@link #getStatusInfo(PrismContainerValueWrapper)} to provide
 * {@link StatusInfo} resolution logic (e.g. using a {@link StatusAwareDataProvider}).</p>
 *
 * <p>This panel does not depend on a specific domain object, so it can be reused
 * anywhere suggestion/status-based rendering is needed.</p>
 */
public abstract class StatusAwareContainerListPanel<C extends Containerable>
        extends MultivalueContainerListPanel<C> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String OP_DETERMINE_STATUSES =
            ResourceObjectTypesPanel.class.getName() + ".determineStatuses";

    protected StatusAwareContainerListPanel(String id, Class<C> type) {
        super(id, type);
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<C>> createProvider() {
        StatusAwareDataFactory.SuggestionsModelDto<C> suggestionsModelDto = getSuggestionsModelDto();
        if (suggestionsModelDto == null) {
            return super.createProvider();
        }
        return new StatusAwareDataProvider<>(this, Model.of(), suggestionsModelDto, false);
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<C>, String> createCheckboxColumn() {
        return isCheckboxColumn() ? new CheckBoxHeaderColumn<>() : super.createCheckboxColumn();
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<C>, String> createNameColumn(
            IModel<String> displayModel,
            GuiObjectColumnType customColumn,
            ExpressionType expression) {

        if (getPathForDisplayName() == null) {
            return super.createNameColumn(displayModel, customColumn, expression);
        }

        return new PrismPropertyWrapperColumn<>(getContainerModel(), getPathForDisplayName(),
                AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId,
                    IModel<PrismContainerValueWrapper<C>> rowModel) {

                PrismContainerValueWrapper<C> wrapper = rowModel.getObject();
                StatusInfo<C> statusInfo = getStatusInfo(wrapper);
                if (statusInfo != null) {
                    OperationResultStatusType status = statusInfo.getStatus();
                    C realValue = wrapper.getRealValue();

                    IModel<String> displayNameModel = () -> getDisplayNameFor(realValue, status);
                    LabelWithBadgePanel label = buildSuggestionNameLabel(componentId, statusInfo, displayNameModel, status);
                    cellItem.add(label);
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
    public void refreshTable(AjaxRequestTarget target) {
        if (getDataProvider() instanceof MultivalueContainerListDataProvider<?> provider) {
            provider.getModel().detach();
        }
        super.refreshTable(target);
    }

    /**
     * Retrieves the {@link StatusInfo} for the given container value.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected @Nullable StatusInfo<C> getStatusInfo(@NotNull PrismContainerValueWrapper<?> value) {
        ISelectableDataProvider<?> provider = getDataProvider();
        if (provider instanceof StatusAwareDataProvider<?> sap) {
            return sap.getSuggestionInfo((PrismContainerValueWrapper) value);
        }
        return null;
    }

    @Nullable
    protected OperationResultStatusType statusFor(
            PrismContainerValueWrapper<?> wrapper) {
        StatusInfo<?> info = getStatusInfo(wrapper);
        return info != null ? info.getStatus() : null;
    }

    /**
     * Applies visual styling and optional auto-refresh behavior for rows
     * that have {@link StatusInfo}.
     */
    @Override
    protected void customProcessNewRowItem(Item<PrismContainerValueWrapper<C>> item,
            @NotNull IModel<PrismContainerValueWrapper<C>> model) {
        StatusInfo<?> status = getStatusInfo(model.getObject());
        if (status != null) {
            item.add(AttributeModifier.replace(
                    "class",
                    SmartIntegrationUtils.SuggestionUiStyle.from(status).rowClass));
            addAutoRefreshBehavior(model.getObject(), item);
        }

        super.customProcessNewRowItem(item, model);
    }

    /**
     * Adds periodic Ajax-based refresh for rows that are "in progress" or need updates.
     */
    protected void addAutoRefreshBehavior(PrismContainerValueWrapper<C> value,
            Item<PrismContainerValueWrapper<C>> item) {
        applySuggestionAutoRefresh(value, item, Duration.ofSeconds(3), this::getStatusInfo, this::refreshTable);
    }

    /**
     * Helper that extracts a display name depending on the value type and status.
     */
    private String getDisplayNameFor(C realValue, OperationResultStatusType status) {
        if (status == OperationResultStatusType.IN_PROGRESS || status == OperationResultStatusType.UNKNOWN) {
            return createStringResource("Generating.suggestion").getString();
        }

        if (realValue instanceof ResourceObjectTypeDefinitionType typeDef) {
            return typeDef.getDisplayName();
        }

        if (realValue instanceof ShadowAssociationTypeDefinitionType assocDef) {
            return assocDef.getName() != null
                    ? assocDef.getName().getLocalPart()
                    : " - ";
        }

        return " - ";
    }

    @Override
    public @NotNull List<InlineMenuItem> getInlineMenuItems() {
        List<InlineMenuItem> inlineMenuItems = super.getInlineMenuItems();
        customizeInlineMenuItems(inlineMenuItems);
        inlineMenuItems.add(createDeleteInlineMenu());
        return inlineMenuItems;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> actions = new ArrayList<>();
        if (showEditInlineMenu()) {
            actions.add(createEditInlineMenu());
        }

        if (showLifecycleStatesInlineMenu()) {
            actions.add(createShowLifecycleStatesInlineMenu());
        }
        return actions;
    }

    protected void customizeInlineMenuItems(@NotNull List<InlineMenuItem> inlineMenuItems) {
        for (InlineMenuItem menuItem : inlineMenuItems) {
            menuItem.setVisibilityChecker((InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
                if (rowModel != null && rowModel.getObject() instanceof PrismContainerValueWrapper<?> rowWrapper) {
                    return statusFor(rowWrapper) == null;
                }
                return true;
            });
        }

        inlineMenuItems.add(createSuggestionOperationInlineMenu(getPageBase(), this::getStatusInfo, this::refreshTable));
        inlineMenuItems.add(createSuggestionDetailsInlineMenu(getPageBase(), this::getStatusInfo));
        inlineMenuItems.add(createSuggestionReviewInlineMenu(getPageBase(), this::getStatusInfo, this::performOnReview));
    }

    @Override
    protected ButtonInlineMenuItem createEditInlineMenu() {
        ButtonInlineMenuItem editInlineMenu = super.createEditInlineMenu();
        editInlineMenu.setLabelVisible(true);
        return editInlineMenu;
    }

    protected void performOnReview(
            @NotNull AjaxRequestTarget target,
            @NotNull PrismContainerValueWrapper<C> valueWrapper,
            @NotNull StatusInfo<?> statusInfo) {
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
                        var popupPanel = new OnePanelPopupPanel(getPageBase().getMainPopupBodyId(), 1100, 600,
                                createStringResource("ContainerWithLifecyclePanel.popup.title")) {

                            @Override
                            protected @NotNull WebMarkupContainer createPanel(String id) {
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

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> bar = new ArrayList<>();
        createNewObjectPerformButton(idButton, bar);
        ToggleCheckBoxPanel togglePanel = createToggleSuggestionVisibilityButton(getPageBase(), idButton, getSwitchSuggestion(),
                this::refreshTable, StatusAwareContainerListPanel.this);
        togglePanel.add(new VisibleBehaviour(this::isToggleSuggestionVisible));
        bar.add(togglePanel);
        return bar;
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

            deleteSingleItem(value);
            value.setSelected(false);
        });
        refreshTable(target);
    }

    protected boolean performOnDeleteSuggestion(
            @NotNull PageBase pageBase,
            AjaxRequestTarget target,
            PrismContainerValueWrapper<C> valueWrapper,
            @Nullable StatusInfo<?> statusInfo) {
        Task task = pageBase.createSimpleTask(OP_DETERMINE_STATUSES);
        OperationResult result = task.getResult();

        return statusInfo != null && removeSuggestionValue(pageBase, target, valueWrapper, statusInfo, task, result);
    }

    @Override
    protected @NotNull String getIconForNewObjectButton() {
        return "fa fa-circle-plus";
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
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected boolean allowEditMultipleValuesAtOnce() {
        return false;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    public boolean displayNoValuePanel() {
        return allowNoValuePanel() && hasNoValues();
    }

    /**
     * Determines whether the panel should display a {@link NoValuePanel}) UI component
     * when there are no values present in the container.
     */
    protected boolean allowNoValuePanel() {
        return true;
    }

    protected boolean hasNoValues() {
        return getDataProvider().size() == 0;
    }

    protected boolean isCheckboxColumn() {
        return true;
    }

    protected boolean showLifecycleStatesInlineMenu() {
        return true;
    }

    protected boolean showEditInlineMenu() {
        return true;
    }

    protected boolean isToggleSuggestionVisible() {
        return !hasNoValues();
    }

    protected IModel<Boolean> getSwitchSuggestion() {
        return Model.of(true);
    }

    protected ItemPath getPathForDisplayName() {
        return null;
    }

    protected abstract StatusAwareDataFactory.SuggestionsModelDto<C> getSuggestionsModelDto();

}
