/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;

import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableRow;

import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ColumnTileTable<O extends Serializable>
        extends TileTablePanel<ColumnTile<O>, O> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_COLUMN_HEADER = "columnHeader";

    private static final String ID_PANEL_TOOLBAR = "panelToolbar";
    private static final String ID_PANEL_TOOLBAR_BUTTONS = "panelToolbarButtons";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";

    private final IModel<List<IColumn<O, String>>> columnsModel;

    public ColumnTileTable(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId,
            IModel<List<IColumn<O, String>>> columnsModel) {
        super(id, viewToggle, tableId);
        this.columnsModel = columnsModel;
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull ColumnTile<O> item) {
        if (item.getValue() instanceof PrismContainerValueWrapper<?> pcvw) {
            switch (pcvw.getStatus()) {
                case DELETED -> tile.add(AttributeModifier.append("class",
                        "border border-danger border-large-left" + applyIfSelectedCssClass(item.getValue())));
                case ADDED -> tile.add(AttributeModifier.append("class",
                        "border border-success border-large-left" + applyIfSelectedCssClass(item.getValue())));
                default ->
                        applyDefaultRowCss(tile, item.getValue(), applyIfSelectedCssClass(item.getValue()), this::getStatusInfo);
            }
        }
    }

    private @NotNull String applyIfSelectedCssClass(O modelObject) {
        return isObjectSelected(modelObject) ? " selected-base" : "";
    }

    @Override
    protected Fragment createHeaderFragment(String id) {
        Fragment fragment = super.createHeaderFragment(id);
        var columnTileHeaderPanel = new ColumnTileHeaderPanel<>(ID_COLUMN_HEADER, this::getDefaultColumns) {
            @Override
            protected void addToolbarButtons(@NotNull RepeatingView repeatingView) {
                if (isCheckboxSelectionEnabled()) {
                    repeatingView.add(createHeaderCheckBoxButton(repeatingView.newChildId()));
                }
                super.addToolbarButtons(repeatingView);
            }
        };
        columnTileHeaderPanel.setOutputMarkupId(true);
        fragment.add(columnTileHeaderPanel);

        var panelToolbar = createPanelToolbar();
        panelToolbar.setOutputMarkupId(true);
        fragment.add(panelToolbar);
        return fragment;
    }

    private @NotNull IsolatedCheckBoxPanel createHeaderCheckBoxButton(String idButton) {
        IModel<Boolean> selectModel = buildHeaderCheckboxModel(this::getCurrentPageItems);

        IsolatedCheckBoxPanel selectCheckbox = new IsolatedCheckBoxPanel(idButton, selectModel) {
            @Override
            public void onUpdate(@NotNull AjaxRequestTarget target) {
                target.add(ColumnTileTable.this);
                refresh(target);
            }
        };

        selectCheckbox.setOutputMarkupId(true);
        selectCheckbox.add(new VisibleBehaviour(() -> isTileViewVisible() && !displayNoValuePanel()));
        selectCheckbox.add(AttributeAppender.replace("class", "btn btn-default"));
        return selectCheckbox;
    }

    private @NotNull IModel<Boolean> buildHeaderCheckboxModel(
            @NotNull IModel<List<O>> currentPageModel) {

        return new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                List<O> current = currentPageModel.getObject();
                return current != null && !current.isEmpty()
                        && current.stream().allMatch(ColumnTileTable::isObjectSelected);
            }

            @Override
            public void setObject(Boolean value) {
                List<O> current = currentPageModel.getObject();
                if (current != null) {
                    boolean sel = Boolean.TRUE.equals(value);
                    current.forEach(o -> setColumnTileSelected(o, sel));
                }
            }
        };
    }

    protected static <O extends Serializable> boolean isObjectSelected(O modelObject) {
        if (modelObject instanceof SelectableRow) {
            return ((SelectableRow<?>) modelObject).isSelected();
        } else if (modelObject instanceof Selectable<?>) {
            return ((Selectable<?>) modelObject).isSelected();
        }
        return false;
    }

    protected static <O extends Serializable> void setColumnTileSelected(O modelObject, boolean selected) {
        if (modelObject instanceof SelectableRow) {
            ((SelectableRow<?>) modelObject).setSelected(selected);
        } else if (modelObject instanceof Selectable<?>) {
            ((Selectable<?>) modelObject).setSelected(selected);
        }
    }

    @Override
    protected Component createTile(String id, IModel<ColumnTile<O>> model) {
        return new ColumnTilePanel<>(id, model) {
            @Override
            protected boolean isCheckboxVisible() {
                return ColumnTileTable.this.isCheckboxSelectionEnabled();
            }
        };
    }

    @Override
    protected ColumnTile<O> createTileObject(O object) {
        return new ColumnTile<>(object, getDefaultColumns());
    }

    public IModel<List<IColumn<O, String>>> getColumnsModel() {
        return columnsModel;
    }

    public List<IColumn<O, String>> getDefaultColumns() {
        List<IColumn<O, String>> columnsModel = new ArrayList<>();

        if (isCheckboxSelectionEnabled() && isTableVisible()) {
            columnsModel.add(0, new CheckBoxHeaderColumn<>());
        }

        columnsModel.addAll(getColumnsModel().getObject());

        if (isActionsColumnEnabled()) {
            initActionColumn(columnsModel);
        }
        return columnsModel;
    }

    private void initActionColumn(List<IColumn<O, String>> columnsModel) {
        IColumn<O, String> linkStyleActionsColumn = createLinkStyleActionsColumn(getPageBase(), getInlineMenuItems());
        if (linkStyleActionsColumn != null) {
            columnsModel.add(linkStyleActionsColumn);
        }
    }

    /**
     * Creates an actions column with inline menu buttons with link style and ellipsis action button.
     */
    public @Nullable IColumn<O, String> createLinkStyleActionsColumn(
            @NotNull PageBase pageBase,
            @NotNull List<InlineMenuItem> allItems) {
        return !allItems.isEmpty() ? new InlineMenuButtonColumn<>(allItems, pageBase) {
            @Override
            public String getCssClass() {
                return "inline-menu-column col";
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
            protected String getInlineMenuItemCssClass(IModel<O> rowModel) {
                return ColumnTileTable.this.getInlineMenuItemCssClass(rowModel);
            }

            @Override
            protected String getAdditionalMultiButtonPanelCssClass() {
                return "justify-content-end";
            }
        } : null;
    }

    protected <R extends Serializable> @NotNull String getInlineMenuItemCssClass(IModel<R> rowModel) {
        return "btn btn-sm text-nowrap d-flex align-items-center";
    }

    protected boolean isCheckboxSelectionEnabled() {
        return true;
    }

    protected boolean isActionsColumnEnabled() {
        return true;
    }

    @Override
    protected String getTileCssStyle() {
        return "min-height: 50px;";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12 h-100 justify-content-center mb-2 p-0 bg-white";
    }

    protected String getTileContainerCssClass() {
        return "d-flex flex-wrap justify-content-left pt-2";
    }

    public @NotNull List<InlineMenuItem> getInlineMenuItems() {
        List<InlineMenuItem> allItems = new ArrayList<>();
        List<InlineMenuItem> menuItems = getDefaultMenuActions();
        if (menuItems != null) {
            allItems.addAll(menuItems);
        }
        allItems.add(createEditInlineMenu());
        return allItems;
    }

    public List<InlineMenuItem> getDefaultMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createDeleteItemMenu());
        return menuItems;
    }

    protected InlineMenuItem createDeleteItemMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("pageAdminFocus.button.delete"))
                .action(createDeleteColumnAction())
                .visibilityChecker(getDefaultMenuVisibilityChecker())
                .buildInlineMenu();
    }

    public InlineMenuItem.@NotNull VisibilityChecker getDefaultMenuVisibilityChecker() {
        return (rowModel, isHeader) -> true;
    }

    protected InlineMenuItem createEditInlineMenu() {
        return InlineMenuItemBuilder.create()
                .label(createStringResource("PageBase.button.edit"))
                .action(createEditColumnAction())
                .headerMenuItem(false)
                .visibilityChecker(getDefaultMenuVisibilityChecker())
                .buildInlineMenu();
    }

    private @NotNull ColumnMenuAction<O> createEditColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }
                editItemPerformed(target, getRowModel());
            }
        };
    }

    public void editItemPerformed(AjaxRequestTarget target, IModel<O> rowModel) {
    }

    @NotNull
    public List<O> getSelectedContainerItems() {
        List<O> selected = new ArrayList<>();
        for (O item : getCurrentPageItems()) {
            if (isObjectSelected(item)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public ColumnMenuAction<O> createDeleteColumnAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() != null) {
                    deleteItemPerformed(target, List.of(getRowModel().getObject()));
                    return;
                }

                final List<O> selected = Optional.of(getSelectedContainerItems()).orElse(List.of());

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
                            deleteItemPerformed(target, getMultiTableModel());
                        }
                        deleteItemPerformed(target, selected);
                    }
                };

                getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    private void deleteItemPerformed(AjaxRequestTarget target, List<O> toDelete) {
        if (noSelectedItemsWarn(getPageBase(), target, toDelete)) {return;}
        toDelete.forEach(this::deleteItemPerformed);
        refreshAndDetach(target);
    }

    protected void deleteItemPerformed(@NotNull O value) {

    }

    public boolean isValidFormComponents() {
        return true;
    }

    protected boolean noSelectedItemsWarn(PageBase pageBase, AjaxRequestTarget target,
            List<O> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            pageBase.warn(pageBase.createStringResource(
                    "MultiSelectContainerActionTileTablePanel.message.noItemsSelected").getString());
            target.add(pageBase.getFeedbackPanel().getParent());
            return true;
        }
        return false;
    }

    public void refreshAndDetach(AjaxRequestTarget target) {
        detachProvider();
        adjustPagingIfEmpty();
        refresh(target);
    }

    protected StringResourceModel deleteConfirmationTitle(int selectedCount) {
        if (selectedCount == 0 && getMultiTableModel().isEmpty()) {
            return createStringResource("ColumnTileTable.deleteConfirmation.title.noItems");
        }

        return selectedCount == 0
                ? createStringResource("ColumnTileTable.deleteConfirmation.title.empty", getMultiTableModel().size())
                : createStringResource("ColumnTileTable.deleteConfirmation.title", selectedCount);
    }

    protected List<O> getMultiTableModel() {
        List<O> allItems = new ArrayList<>();
        ISortableDataProvider<O, String> provider = getProvider();
        long size = provider.size();
        Iterator<? extends O> it = provider.iterator(0, size);
        it.forEachRemaining(allItems::add);
        return allItems;
    }

    /**
     * Retrieves the {@link StatusInfo} for the given container value.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Nullable
    public StatusInfo<?> getStatusInfo(@NotNull O value) {
        if (getProvider() instanceof StatusAwareDataProvider<?> sap) {
            return sap.getSuggestionInfo((PrismContainerValueWrapper) value);
        }
        return null;
    }

    private void detachProvider() {
        ISortableDataProvider<O, String> provider = getProvider();
        if (provider != null) {
            provider.detach();
        }

        if (provider instanceof MultivalueContainerListDataProvider<?> mvProvider) {
            mvProvider.getModel().detach();
        }
    }

    /**
     * Applies default CSS class to the tile including the base CSS and the status-based CSS.
     */
    public <T> void applyDefaultRowCss(
            @NotNull Component tile,
            @NotNull O value,
            @NotNull String baseCss,
            @NotNull SerializableFunction<O, @Nullable StatusInfo<T>> getStatusInfoFn) {
        if (value instanceof PrismContainerValueWrapper<?>) {
            StatusInfo<T> statusInfo = getStatusInfoFn.apply(value);
            if (statusInfo != null && statusInfo.getStatus() != null) {
                String styleClass = SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo).tileClass;
                tile.add(AttributeModifier.append("class", baseCss + " border-large-left " + styleClass));
            }
        }
    }

    @NotNull
    protected AjaxIconButton createNewObjectPerformButton(String idButton) {
        AjaxIconButton newObjectButton = new AjaxIconButton(idButton,
                Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE),
                getNewObjectButtonTitle()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToLastPage();
                onCreateNewObjectPerform(target);
            }
        };

        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(AttributeAppender.replace("class", "btn btn-link rounded ml-auto"));
        newObjectButton.add(new VisibleBehaviour(this::isNewObjectCreationEnabled));
        return newObjectButton;
    }

    protected StringResourceModel getNewObjectButtonTitle() {
        return createStringResource("ColumnTileTable.button.newObject");
    }

    protected void onCreateNewObjectPerform(AjaxRequestTarget target) {

    }

    protected List<Component> createToolbarButtonsList(String idButton) {
        return new ArrayList<>();
    }

    protected void initPanelToolbarButtons(@NotNull RepeatingView toolbar) {
        AjaxIconButton newObjectPerformButton = createNewObjectPerformButton(toolbar.newChildId());
        toolbar.add(newObjectPerformButton);
    }

    @Override
    protected Component createToolbarButtons(String id) {
        ButtonBar<Containerable, SelectableRow<?>> buttonBar = new ButtonBar<>(id, ID_BUTTON_BAR,
                ColumnTileTable.this, createToolbarButtonsList(ID_BUTTON));
        buttonBar.setOutputMarkupId(true);
        buttonBar.add(new VisibleBehaviour(this::isToolbarButtonsVisible));
        return buttonBar;
    }

    private @NotNull Component createPanelToolbar() {
        WebMarkupContainer panelToolbar = new WebMarkupContainer(ID_PANEL_TOOLBAR);
        panelToolbar.setOutputMarkupId(true);

        RepeatingView toolbar = new RepeatingView(ColumnTileTable.ID_PANEL_TOOLBAR_BUTTONS);
        initPanelToolbarButtons(toolbar);
        panelToolbar.add(toolbar);
        panelToolbar.add(new VisibleBehaviour(this::isPanelToolbarVisible));
        return panelToolbar;
    }

    protected boolean isToolbarButtonsVisible() {
        return true;
    }

    protected boolean isPanelToolbarVisible() {
        return true;
    }

    protected boolean isNewObjectCreationEnabled() {
        return true;
    }

    public void setDefaultPagingSize(Integer pageItemSize) {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(getTableId(), pageItemSize);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected IModel<Search> createSearchModel() {
        return super.createSearchModel();
    }

    @Override
    public boolean displayNoValuePanel() {
//        return getProvider().size() == 0;
        return false; //disable for now
    }

    @Override
    protected List<Component> createNoValueButtonToolbar(String id) {
        List<Component> buttons = new ArrayList<>();
        AjaxIconButton newObjectPerformButton = createNewObjectPerformButton(id);
        buttons.add(newObjectPerformButton);
        return buttons;
    }
}
