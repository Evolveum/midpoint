/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;

import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsConfirmationPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.createLinkStyleActionsColumn;
import static com.evolveum.midpoint.gui.impl.util.StatusInfoTableUtil.createToggleSuggestionVisibilityButton;

public abstract class MultiSelectContainerActionTileTablePanel<E extends Serializable, C extends Containerable, T extends TemplateTile<PrismContainerValueWrapper<C>>>
        extends MultiSelectTileTablePanel<E, PrismContainerValueWrapper<C>, T> {

    private IModel<Boolean> switchToggleModel = Model.of(Boolean.TRUE);

    public MultiSelectContainerActionTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId,
            IModel<ViewToggle> toggleView) {
        super(id, toggleView, tableId);
    }

    public MultiSelectContainerActionTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId,
            IModel<ViewToggle> toggleView,
            IModel<Boolean> switchToggleModel) {
        super(id, toggleView, tableId);
        this.switchToggleModel = switchToggleModel;
    }

    @Override
    protected void customizeNewRowItem(PrismContainerValueWrapper<C> value, Item<PrismContainerValueWrapper<C>> item) {
        super.customizeNewRowItem(value, item);
        updateRowCssBasedValueStatus(item, value, false);
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull T item) {
        updateRowCssBasedValueStatus(tile, item.getValue(), true);
    }

    @Override
    protected MultivalueContainerListDataProvider<C> createProvider() {
        return createDataProvider();
    }

    protected abstract MultivalueContainerListDataProvider<C> createDataProvider();

    @Override
    protected void togglePanelItemSelectPerformed(
            AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
        ViewToggle value = item.getObject().getValue();
        add(AttributeModifier.replace("class", Objects.equals(value, ViewToggle.TABLE) ? "card" : ""));
        super.togglePanelItemSelectPerformed(target, item);
        refresh(target);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        buttonsList.add(createTableActionToolbar(idButton));
        buttonsList.add(createSuggestObjectButton(idButton));
        ToggleCheckBoxPanel toggleSuggestionButton = createToggleSuggestionButton(idButton, switchToggleModel);
        toggleSuggestionButton.add(new VisibleBehaviour(() -> !displayNoValuePanel()));
        buttonsList.add(toggleSuggestionButton);

        AjaxIconButton newObjectPerformButton = createNewObjectPerformButton(idButton, null);
        newObjectPerformButton.add(AttributeModifier.replace("class",
                "text-nowrap btn btn-primary rounded text-nowrap mx-3"));
        newObjectPerformButton.add(new VisibleBehaviour(this::displayNoValuePanel));
        buttonsList.add(0, newObjectPerformButton);
        return buttonsList;
    }

    @Override
    protected WebMarkupContainer createHeaderContainer() {
        WebMarkupContainer headerContainer = super.createHeaderContainer();
        headerContainer.add(createNewObjectPerformButton("newObjectButton", getModelObject()));
        return headerContainer;
    }

    protected RepeatingView createTableActionToolbar(String id) {
        RepeatingView toolbar = new RepeatingView(id);
        toolbar.add(createHeaderCheckBoxButton(toolbar.newChildId()));
        toolbar.add(createDropDownActionButton(toolbar.newChildId()));
        toolbar.setOutputMarkupId(true);
        return toolbar;
    }

    @Override
    protected final @NotNull List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<C>, String>> columns = new ArrayList<>();

        if (showCheckboxColumn()) {
            columns.add(new CheckBoxHeaderColumn<>());
        }

        List<IColumn<PrismContainerValueWrapper<C>, String>> domain = createDomainColumns();
        if (domain != null && !domain.isEmpty()) {
            columns.addAll(domain);
        }

        if (showActionsColumn()) {
            @Nullable IColumn<PrismContainerValueWrapper<C>, String> actions = createLinkStyleActionsColumn(
                    getPageBase(), getInlineMenuItems(null));
            if (actions != null) {
                columns.add(actions);
            }
        }

        return columns;
    }

    /** Subclasses provide the domain columns only. */
    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> createDomainColumns();

    public @NotNull List<InlineMenuItem> getInlineMenuItems(PrismContainerValueWrapper<C> tileModel) {
        List<InlineMenuItem> allItems = new ArrayList<>();
        List<InlineMenuItem> menuItems = getDefaultMenuActions(tileModel);
        if (menuItems != null) {
            allItems.addAll(menuItems);
        }
        allItems.add(createEditInlineMenu(tileModel));
        allItems.add(createDuplicateInlineMenu(tileModel));
        return allItems;
    }

    public List<InlineMenuItem> getDefaultMenuActions(PrismContainerValueWrapper<C> model) {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createDeleteItemMenu(model));
        return menuItems;
    }

    public void updateRowCssBasedValueStatus(
            @NotNull Component component,
            @NotNull PrismContainerValueWrapper<C> value,
            boolean isTile) {
        if (isTile) {
            switch (value.getStatus()) {
                case DELETED ->
                        component.add(AttributeModifier.replace("class", "card rounded h-100 m-0 border border-danger border-large-left"));
                case ADDED ->
                        component.add(AttributeModifier.replace("class", "card rounded h-100 m-0 border border-success border-large-left"));
                default -> component.add(AttributeModifier.replace("class", "card rounded h-100 m-0"));
            }
            return;
        }

        switch (value.getStatus()) {
            case ADDED -> component.add(AttributeModifier.append("class", "table-success"));
            case DELETED -> component.add(AttributeModifier.append("class", "table-danger"));
        }
    }

    protected List<PrismContainerValueWrapper<C>> getMultiTableModel() {
        MultivalueContainerListDataProvider<C> provider = (MultivalueContainerListDataProvider<C>) getProvider();
        return provider.getModel().getObject();
    }

    private @NotNull DropdownButtonPanel createDropDownActionButton(String idButton) {
        DropdownButtonDto model = new DropdownButtonDto(null, null, null, getDefaultMenuActions(null));
        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(idButton, model) {
            @Serial private static final long serialVersionUID = 1L;

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return "btn btn-default mr-2";
            }

            @Override
            protected @NotNull String getSpecialDropdownMenuClass() {
                return "dropdown-menu-end";
            }
        };

        inlineMenu.setOutputMarkupPlaceholderTag(true);
        inlineMenu.setOutputMarkupId(true);
        inlineMenu.add(AttributeAppender.append("class", "mr-2"));
        inlineMenu.add(new VisibleBehaviour(() -> isTileViewVisible() && !displayNoValuePanel()));
        inlineMenu.setRenderBodyOnly(true);
        return inlineMenu;
    }

    @NotNull
    protected AjaxIconButton createNewObjectPerformButton(String idButton, PrismContainerValueWrapper<C> modelObject) {
        AjaxIconButton newObjectButton = new AjaxIconButton(idButton,
                Model.of(GuiStyleConstants.CLASS_ADD_NEW_OBJECT),
                getNewObjectButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToLastPage();
                onCreateNewObjectPerform(target);
            }
        };

        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(AttributeAppender.replace("class",
                "text-nowrap btn btn-primary rounded ml-auto text-nowrap mr-2"));
        return newObjectButton;
    }

    protected IModel<String> getNewObjectButtonLabel() {
        return createStringResource("SmartCorrelationTable.button.addNewCorrelationItem");
    }

    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
        newItemPerformed(null, target, null, false, null);
        refreshAndDetach(target);
    }

    protected InlineMenuItem createDeleteItemMenu(PrismContainerValueWrapper<C> model) {
        return new InlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                ColumnMenuAction<PrismContainerValueWrapper<C>> deleteColumnAction = createDeleteColumnAction();
                if (model != null) {
                    deleteColumnAction.setRowModel(() -> model);
                }
                return deleteColumnAction;
            }
        };
    }

    @Contract("_ -> new")
    protected @NotNull InlineMenuItem createDuplicateInlineMenu(PrismContainerValueWrapper<C> tileModel) {
        return new InlineMenuItem(getPageBase().createStringResource("DuplicationProcessHelper.menu.duplicate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                ColumnMenuAction<PrismContainerValueWrapper<C>> duplicateColumnAction = DuplicationProcessHelper
                        .createDuplicateColumnAction(getPageBase(),
                                (value, target) -> newItemPerformed(value, target, null, true, null));

                if (tileModel != null) {
                    duplicateColumnAction.setRowModel(() -> tileModel);
                }

                return duplicateColumnAction;
            }

            @Override
            public boolean showConfirmationDialog() {
                return false;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
    }

    protected PrismContainerValueWrapper<C> createNewValue(PrismContainerValue<C> value, AjaxRequestTarget target) {
        PrismContainerWrapper<C> container = getContainerModel().getObject();
        PrismContainerValue<C> newValue = value;
        if (newValue == null) {
            newValue = container.getItem().createNewValue();
        }
        return createNewItemContainerValueWrapper(newValue, container, target);
    }

    public PrismContainerValueWrapper<C> createNewItemContainerValueWrapper(
            PrismContainerValue<C> newItem,
            PrismContainerWrapper<C> model, AjaxRequestTarget target) {

        return WebPrismUtil.createNewValueWrapper(model, newItem, getPageBase(), target);
    }

    protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec,
            boolean isDuplicate, StatusInfo<?> statusInfo) {
    }

    protected void setDefaultPagingSize(UserProfileStorage.@NotNull TableId tableId, Integer pageItemSize) {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(tableId, pageItemSize);
    }

    public ColumnMenuAction<PrismContainerValueWrapper<C>> createDeleteColumnAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() != null) {
                    deleteItemPerformed(target, List.of(getRowModel().getObject()), true);
                    return;
                }

                final List<PrismContainerValueWrapper<C>> selected = Optional.ofNullable(getSelectedContainerItemsModel()
                        .getObject()).orElse(List.of());

                ConfirmationPanel dialog = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        deleteConfirmationTitle(selected.size())) {

                    @Override
                    protected IModel<String> createNoLabel() {
                        return selected.isEmpty()
                                ? createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.cancel")
                                : super.createNoLabel();
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        if (selected.isEmpty()) {
                            deleteItemPerformed(target, getMultiTableModel(), true);
                        }
                        deleteItemPerformed(target, selected, true);
                    }
                };

                getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    protected StringResourceModel deleteConfirmationTitle(int selectedCount) {
        if (selectedCount == 0 && getMultiTableModel().isEmpty()) {
            return createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.title.noItems");
        }

        return selectedCount == 0
                ? createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.title.empty", getMultiTableModel().size())
                : createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.title", selectedCount);
    }

    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<C>> toDelete, boolean refresh) {
        if (noSelectedItemsWarn(getPageBase(), target, toDelete)) {return;}
        toDelete.forEach(this::resolveDeletedItem);
        if (refresh) {
            refreshAndDetach(target);
        }
    }

    protected static <C extends Containerable> boolean noSelectedItemsWarn(PageBase pageBase, AjaxRequestTarget target,
            List<PrismContainerValueWrapper<C>> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            pageBase.warn(pageBase.createStringResource(
                    "MultiSelectContainerActionTileTablePanel.message.noItemsSelected").getString());
            target.add(pageBase.getFeedbackPanel().getParent());
            return true;
        }
        return false;
    }

    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<C> value) {
        if (value.getStatus() == ValueStatus.ADDED) {
            IModel<PrismContainerWrapper<C>> containerModel = getContainerModel();
            PrismContainerWrapper<C> wrapper = containerModel.getObject();
            if (wrapper != null) {
                wrapper.getValues().remove(value);
            }
        } else {
            value.setStatus(ValueStatus.DELETED);
        }
        value.setSelected(false);
    }

    protected ButtonInlineMenuItem createEditInlineMenu(PrismContainerValueWrapper<C> tileModel) {
        return new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction(tileModel);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
    }

    public ColumnMenuAction<PrismContainerValueWrapper<C>> createEditColumnAction(PrismContainerValueWrapper<C> tileModel) {
        ColumnMenuAction<PrismContainerValueWrapper<C>> columnMenuAction = new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }
                editItemPerformed(target, getRowModel(), false);
            }
        };

        if (tileModel != null) {
            columnMenuAction.setRowModel(() -> tileModel);
        }

        return columnMenuAction;
    }

    public void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<C>> rowModel,
            boolean isDuplicate) {
    }

    protected IModel<List<PrismContainerValueWrapper<C>>> getSelectedContainerItemsModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<C>> load() {
                List<PrismContainerValueWrapper<C>> all = getCurrentPageItems();
                if (all == null || all.isEmpty()) {
                    return List.of();
                }
                return all.stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .toList();
            }
        };
    }

    private @NotNull IsolatedCheckBoxPanel createHeaderCheckBoxButton(String idButton) {
        IModel<Boolean> selectModel = buildHeaderCheckboxModel(this::getCurrentPageItems);

        IsolatedCheckBoxPanel selectCheckbox = new IsolatedCheckBoxPanel(idButton, selectModel) {
            @Override
            public void onUpdate(@NotNull AjaxRequestTarget target) {
                target.add(MultiSelectContainerActionTileTablePanel.this);
                refresh(target);
            }
        };

        selectCheckbox.setOutputMarkupId(true);
        selectCheckbox.add(new VisibleBehaviour(() -> isTileViewVisible() && !displayNoValuePanel()));
        selectCheckbox.add(AttributeAppender.replace("class", "btn btn-default"));
        return selectCheckbox;
    }

    private @NotNull IModel<Boolean> buildHeaderCheckboxModel(
            @NotNull IModel<List<PrismContainerValueWrapper<C>>> currentPageModel) {

        return new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                List<PrismContainerValueWrapper<C>> current = currentPageModel.getObject();
                return current != null && !current.isEmpty()
                        && current.stream().allMatch(PrismContainerValueWrapper::isSelected);
            }

            @Override
            public void setObject(Boolean value) {
                List<PrismContainerValueWrapper<C>> current = currentPageModel.getObject();
                if (current != null) {
                    boolean sel = Boolean.TRUE.equals(value);
                    current.forEach(v -> v.setSelected(sel));
                }
            }
        };
    }

    @NotNull
    protected AjaxIconButton createSuggestObjectButton(String idButton) {
        AjaxIconButton suggestObjectButton = new AjaxIconButton(idButton, Model.of("fa-solid fa-wand-magic-sparkles"),
                createStringResource("SmartIntegration.suggestNew")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                RequestDetailsConfirmationPanel dialog = new RequestDetailsConfirmationPanel(getPageBase().getMainPopupBodyId(),
                        buildSmartPermissionRecordDto()) {

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        onSuggestNewPerformed(target);
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };

        suggestObjectButton.showTitleAsLabel(true);
        suggestObjectButton.add(AttributeAppender.replace("class", "btn btn-default rounded mr-2"));
        suggestObjectButton.add(new VisibleBehaviour(this::isSuggestButtonVisible));
        suggestObjectButton.setOutputMarkupId(true);
        return suggestObjectButton;
    }

    protected IModel<RequestDetailsRecordDto> buildSmartPermissionRecordDto() {
        return () -> new RequestDetailsRecordDto(null, null);
    }

    @NotNull
    protected ToggleCheckBoxPanel createToggleSuggestionButton(String idButton, IModel<Boolean> switchToggleModel) {
        return createToggleSuggestionVisibilityButton(getPageBase(), idButton, switchToggleModel,
                this::refreshAndDetach, getComponentToFocusAfterAiToggle());
    }

    protected Component getComponentToFocusAfterAiToggle() {
        return this;
    }

    protected AjaxIconButton createDiscardButton(String id, IModel<PrismContainerValueWrapper<C>> rowModel) {
        AjaxIconButton discardButton = new AjaxIconButton(id, Model.of("fa fa-solid fa-x"),
                createStringResource("SmartCorrelationTilePanel.discardButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteItemPerformed(target, Collections.singletonList(rowModel.getObject()), true);
            }
        };
        discardButton.setOutputMarkupId(true);
        discardButton.add(new TooltipBehavior());
        discardButton.add(AttributeModifier.replace("class", getDiscardButtonCssClass()));
        discardButton.showTitleAsLabel(true);
        return discardButton;
    }

    protected String getDiscardButtonCssClass() {
        return "col-auto px-4 btn btn-default btn-sm rounded ml-auto";
    }

    protected AjaxIconButton createAcceptButton(String id, IModel<PrismContainerValueWrapper<C>> rowModel) {
        AjaxIconButton acceptButton = new AjaxIconButton(id, Model.of("fa fa-check"),
                createStringResource("SmartCorrelationTilePanel.acceptButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAcceptPerformed(target, rowModel);
            }
        };
        acceptButton.setOutputMarkupId(true);
        acceptButton.add(new TooltipBehavior());
        acceptButton.add(AttributeModifier.replace("class", getAcceptButtonCssClass()));
        acceptButton.showTitleAsLabel(true);
        return acceptButton;
    }

    protected void onAcceptPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> toAccept) {
        refreshAndDetach(target);
    }

    protected String getAcceptButtonCssClass() {
        return "col-auto px-4 btn btn-primary btn-sm rounded ml-auto";
    }

    protected boolean isDuplicationSupported() {
        return true;
    }

    protected boolean isSuggestButtonVisible() {
        return false;
    }

    protected void onSuggestNewPerformed(AjaxRequestTarget target) {
    }

    protected boolean isToggleSuggestionVisible() {
        return getSwitchToggleModel().getObject().equals(Boolean.TRUE) && !displayNoValuePanel();
    }

    protected IModel<Boolean> getSwitchToggleModel() {
        return switchToggleModel;
    }

    protected @Nullable String getStatusInfoToken(
            PrismContainerValueWrapper<C> value) {
        if (getProvider() instanceof StatusAwareDataProvider<C> provider) {
            StatusInfo<?> suggestionInfo = provider.getSuggestionInfo(value);
            if (suggestionInfo != null) {
                return suggestionInfo.getToken();
            }
        }
        return null;
    }

    protected @Nullable StatusInfo<?> getStatusInfoObject(
            PrismContainerValueWrapper<C> value) {
        if (getProvider() instanceof StatusAwareDataProvider<C> provider) {
            StatusInfo<?> suggestionInfo = provider.getSuggestionInfo(value);
            if (suggestionInfo != null) {
                return suggestionInfo;
            }
        }
        return null;
    }

    public void refreshAndDetach(AjaxRequestTarget target) {
        getTilesModel().detach();

        if (getProvider() instanceof MultivalueContainerListDataProvider<C> provider) {
            provider.getModel().detach();
        }
        adjustPagingIfEmpty();
        super.refresh(target);
    }

    @Contract("_, _ -> new")
    public static <C extends Containerable> @NotNull IModel<PrismContainerWrapper<C>> getContainerModel(PrismContainerWrapper<C> value, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerWrapper(Model.of(value), path);
    }

    protected IModel<PrismContainerWrapper<C>> getContainerModel() {
        return null;
    }

    @Override
    protected String getAdditionalTableCssClasses() {
        return "table-td-middle";
    }

    @Override
    protected String getAdditionalFooterCss() {
        return "bg-white border-top";
    }

    @Override
    protected String getTilesContainerAdditionalClass() {
        return "";
    }

    @Override
    protected String getTileCssStyle() {
        return "min-height: 450px;";
    }

    @Override
    protected String getTileContainerCssClass() {
        return "row justify-content-left pt-2 ";
    }

    @Override
    protected String getAdditionalBoxCssClasses() {
        return " m-0";
    }

    @Override
    protected String getTilesFooterCssClasses() {
        return "pt-1 border-0";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12 col-sm-12 col-md-6 col-lg-3 p-2";
    }

    @Override
    protected String getAdditionalHeaderContainerCssClasses() {
        return isTileViewVisible() ? "border-0 p-0" : "p-3";
    }

    @Override
    public boolean displayNoValuePanel() {
        return getProvider().size() == 0;
    }

    @Override
    protected boolean isSelectedItemsPanelVisible() {
        return false;
    }

    @Override
    protected boolean isTableRowSelectable() {
        return false;
    }

    @Override
    protected boolean isTogglePanelVisible() {
        return true;
    }

    protected boolean showCheckboxColumn() {
        return true;
    }

    protected boolean showActionsColumn() {
        return true;
    }

}
