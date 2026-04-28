/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.GroupedMappingDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.ValidatorAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Column-based tile table that separates:
 *
 * <ul>
 *   <li><b>O</b> - the primary row object handled by the table, selection, paging and actions</li>
 *   <li><b>PV</b> - the delegated value rendered by reusable Wicket columns</li>
 * </ul>
 *
 * <p>The primary row object must implement {@link ColumnValueProvider}, which provides
 * the delegated value used when rendering tile columns.</p>
 *
 * @param <O> primary row object type
 * @param <PV> delegated value type rendered by columns
 */
public abstract class ColumnTileTable<O extends ColumnValueProvider<PV>, PV extends Serializable>
        extends TileTablePanel<ColumnTile<O, PV>, O> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_COLUMN_HEADER = "columnHeader";
    private static final String ID_PANEL_TOOLBAR = "panelToolbar";
    private static final String ID_PANEL_TOOLBAR_BUTTONS = "panelToolbarButtons";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";

    private final IModel<List<IColumn<PV, String>>> columnsModel;

    public ColumnTileTable(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId,
            IModel<List<IColumn<PV, String>>> columnsModel) {
        super(id, viewToggle, tableId);
        this.columnsModel = columnsModel;
    }

    @Override
    protected void customizeTileItemCss(Component tile, @NotNull ColumnTile<O, PV> item) {
        PV columnValue = item.getValue().getColumnValue();

        if (columnValue instanceof PrismContainerValueWrapper<?> pcvw) {
            switch (pcvw.getStatus()) {
                case DELETED -> tile.add(AttributeModifier.append(
                        "class", "border border-danger border-large-left" + applyIfSelectedCssClass(item.getValue())));
                case ADDED -> tile.add(AttributeModifier.append(
                        "class", "border border-success border-large-left" + applyIfSelectedCssClass(item.getValue())));
                default -> applyDefaultRowCss(
                        tile,
                        item.getValue(),
                        applyIfSelectedCssClass(item.getValue()),
                        this::getStatusInfoFromObject);
            }
        }
    }

    private @NotNull String applyIfSelectedCssClass(O modelObject) {
        return isObjectSelected(modelObject) ? " selected-base" : "";
    }

    @Override
    protected Fragment createHeaderFragment(String id) {
        Fragment fragment = super.createHeaderFragment(id);

        var columnTileHeaderPanel = new ColumnTileHeaderPanel<O, PV>(ID_COLUMN_HEADER, this::getDefaultColumns) {
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

        Component panelToolbar = createPanelToolbar();
        panelToolbar.setOutputMarkupId(true);
        fragment.add(panelToolbar);

        return fragment;
    }

    private @NotNull IsolatedCheckBoxPanel createHeaderCheckBoxButton(String idButton) {
        IModel<Boolean> selectModel = buildHeaderCheckboxModel(this::getCurrentPageItems);

        IsolatedCheckBoxPanel selectCheckbox = new IsolatedCheckBoxPanel(idButton, selectModel) {
            @Override
            public void onUpdate(@NotNull AjaxRequestTarget target) {
                updateTileView(target);
            }
        };

        selectCheckbox.setOutputMarkupId(true);
        selectCheckbox.add(new VisibleBehaviour(() -> isTileViewVisible() && !displayNoValuePanel()));
        selectCheckbox.add(AttributeAppender.replace("class", "btn btn-default"));
        return selectCheckbox;
    }

    private void updateTileCheckboxes(@NotNull AjaxRequestTarget target) {
        PageableListView<?, ?> tiles = getTiles();

        tiles.visitChildren(Component.class, (component, visit) -> {
            if (isRefreshableTileComponent(component)) {
                target.add(component);
            }
        });

    }

    private boolean isRefreshableTileComponent(@NotNull Component component) {
        return component.getOutputMarkupId()
                && (component instanceof ColumnTilePanel<?, ?, ?>
                || component instanceof MappingSuggestionGroupColumnTilePanel<?, ?, ?>);
    }

    private @NotNull IModel<Boolean> buildHeaderCheckboxModel(@NotNull IModel<List<O>> currentPageModel) {
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
    protected Component createTile(String id, @NotNull IModel<ColumnTile<O, PV>> model) {
        O tileModel = model.getObject().getValue();
        return new ColumnTilePanel<>(id, model, tileModel::getColumnValue) {
            @Override
            protected boolean isCheckboxVisible() {
                return ColumnTileTable.this.isCheckboxSelectionEnabled();
            }
        };
    }

    @Override
    protected ColumnTile<O, PV> createTileObject(O object) {
        return new ColumnTile<>(object, getDefaultColumns());
    }

    public IModel<List<IColumn<PV, String>>> getColumnsModel() {
        return columnsModel;
    }

    public List<IColumn<PV, String>> getDefaultColumns() {
        List<IColumn<PV, String>> result = new ArrayList<>(getColumnsModel().getObject());

        if (isActionsColumnEnabled()) {
            initActionColumn(result);
        }

        return result;
    }

    private void initActionColumn(List<IColumn<PV, String>> columns) {
        IColumn<PV, String> actionsColumn = createLinkStyleActionsColumn(getPageBase(), getInlineMenuItems());
        if (actionsColumn != null) {
            columns.add(actionsColumn);
        }
    }

    public @Nullable IColumn<PV, String> createLinkStyleActionsColumn(
            @NotNull PageBase pageBase,
            @NotNull List<InlineMenuItem> allItems) {
        return !allItems.isEmpty() ? new InlineMenuButtonColumn<>(allItems, pageBase) {
            @Override
            protected boolean showInlineMenuIcon() {
                return true;
            }

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
            protected String getInlineMenuItemCssClass(IModel<PV> rowModel) {
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
                .icon("fa fa-trash text-danger")
                .additionalCssClass("text-danger")
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
                .icon("fa fa-edit")
                .label(createStringResource("PageBase.button.edit"))
                .action(createEditColumnAction())
                .headerMenuItem(false)
                .visibilityChecker(getDefaultMenuVisibilityChecker())
                .buildInlineMenu();
    }

    private @NotNull ColumnMenuAction<PV> createEditColumnAction() {
        return new ColumnMenuAction<>() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    return;
                }

                O rowObject = findRowObject(getRowModel().getObject());
                if (rowObject != null) {
                    editItemPerformed(target, Model.of(rowObject));
                }
            }
        };
    }

    protected @Nullable O findRowObject(@NotNull PV columnValue) {
        for (O row : getCurrentPageItems()) {
            if (Objects.equals(row.getColumnValue(), columnValue)) {
                return row;
            }
        }
        return null;
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

    public ColumnMenuAction<PV> createDeleteColumnAction() {
        return new ColumnMenuAction<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() != null) {
                    deleteItemPerformed(getRowModel().getObject());
                    refreshAndDetach(target);
                    return;
                }

                final List<O> selected = new ArrayList<>(Optional.of(getSelectedContainerItems()).orElse(List.of()));
                final List<O> allItems = new ArrayList<>(Optional.ofNullable(getMultiTableModel()).orElse(List.of()));

                final int selectedCount = selected.size();
                final int allCount = allItems.size();

                ConfirmationPanel dialog = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        deleteConfirmationTitle(selectedCount, allCount)) {

                    @Override
                    protected IModel<String> createNoLabel() {
                        return selectedCount == 0
                                ? createStringResource("MultiSelectContainerActionTileTablePanel.deleteConfirmation.cancel")
                                : super.createNoLabel();
                    }

                    @Override
                    protected boolean isYesButtonVisible() {
                        return selectedCount > 0 || allCount > 0;
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        if (selected.isEmpty()) {
                            deleteItemPerformed(target, allItems);
                        } else {
                            deleteItemPerformed(target, selected);
                        }
                        getPageBase().hideMainPopup(target);
                    }
                };

                getPageBase().showMainPopup(dialog, target);
            }
        };
    }

    public void deleteItemPerformed(AjaxRequestTarget target, List<O> toDelete) {
        if (noSelectedItemsWarn(getPageBase(), target, toDelete)) {
            return;
        }
        for (O o : toDelete) {
            List<PV> columnsValues = o.getColumnsValues();
            columnsValues.forEach(this::deleteItemPerformed);
        }

        refreshAndDetach(target);
    }

    protected void deleteItemPerformed(@NotNull PV value) {
    }

    public boolean isValidFormComponents(@Nullable AjaxRequestTarget target) {
        AtomicBoolean valid = new AtomicBoolean(true);

        visitChildren(FormComponent.class, (component, visit) -> {
            FormComponent<?> formComponent = (FormComponent<?>) component;

            if (!formComponent.isVisibleInHierarchy() || !formComponent.isEnabledInHierarchy()) {
                return;
            }

            if (formComponent.hasErrorMessage()) {
                valid.set(false);
                updateValidatorComponent(target, valid, formComponent);
                return;
            }

            enableUseModelForNotNullValidators(formComponent);
            formComponent.validate();

            if (formComponent.hasErrorMessage()) {
                valid.set(false);
                updateValidatorComponent(target, valid, formComponent);
            }
        });

        return valid.get();
    }

    private static void updateValidatorComponent(
            @Nullable AjaxRequestTarget target,
            AtomicBoolean valid,
            FormComponent<?> formComponent) {
        if (target != null) {
            target.add(formComponent);

            InputPanel inputPanel = formComponent.findParent(InputPanel.class);
            if (inputPanel != null) {
                target.add(inputPanel);

                if (inputPanel.getParent() != null) {
                    target.addChildren(inputPanel.getParent(), FeedbackLabels.class);
                }
            }
        }
    }

    private void enableUseModelForNotNullValidators(@NotNull FormComponent<?> formComponent) {
        formComponent.getBehaviors().stream()
                .filter(ValidatorAdapter.class::isInstance)
                .map(ValidatorAdapter.class::cast)
                .map(ValidatorAdapter::getValidator)
                .filter(NotNullValidator.class::isInstance)
                .map(NotNullValidator.class::cast)
                .forEach(validator -> validator.setUseModel(true));
    }

    protected boolean noSelectedItemsWarn(PageBase pageBase, AjaxRequestTarget target, List<O> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            pageBase.warn(pageBase.createStringResource(
                    "MultiSelectContainerActionTileTablePanel.message.noItemsSelected").getString());
            target.add(pageBase.getFeedbackPanel());
            return true;
        }
        return false;
    }

    public void refreshAndDetach(AjaxRequestTarget target) {
        detachProvider();
        adjustPagingIfEmpty();
        refresh(target);
    }

    protected StringResourceModel deleteConfirmationTitle(int selectedCount, int allCount) {
        if (selectedCount == 0 && allCount == 0) {
            return createStringResource("ColumnTileTable.deleteConfirmation.title.noItems");
        }

        return selectedCount == 0
                ? createStringResource("ColumnTileTable.deleteConfirmation.title.empty", allCount)
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Nullable
    public StatusInfo<?> getStatusInfoFromObject(@NotNull O value) {
        PV columnValue = value.getColumnValue();

        if (!(columnValue instanceof PrismContainerValueWrapper<?> wrapper)) {
            return null;
        }

        if (getProvider() instanceof StatusAwareDataProvider sap) {
            return sap.getSuggestionInfo(wrapper);
        }

        if (getProvider() instanceof GroupedMappingDataProvider gmdp) {
            return gmdp.getSuggestionInfo(wrapper);
        }

        return null;
    }

    @Nullable
    public StatusInfo<?> getStatusInfo(@Nullable PV value) {
        if (value instanceof PrismContainerValueWrapper<?> wrapper
                && getProvider() instanceof GroupedMappingDataProvider gmdp) {
            return gmdp.getSuggestionInfo(wrapper);
        }
        return null;
    }

    private void detachProvider() {
        ISortableDataProvider<O, String> provider = getProvider();
        if (provider == null) {
            return;
        }

        provider.detach();

        if (provider instanceof GroupedMappingDataProvider groupedProvider) {
            groupedProvider.reset();
            ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> delegate = groupedProvider.getDelegateProvider();

            detachDelegateProvider(delegate);
            return;
        }

        //noinspection rawtypes
        if (provider instanceof MultivalueContainerListDataProvider multivalueProvider) {
            multivalueProvider.getModel().detach();
        }
    }

    private void detachDelegateProvider(
            ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> delegateProvider) {

        if (delegateProvider instanceof StatusAwareDataProvider<MappingType> statusAwareProvider) {
            IModel<List<PrismContainerValueWrapper<MappingType>>> model = statusAwareProvider.getModel();

            if (model instanceof LoadableModel<List<PrismContainerValueWrapper<MappingType>>> loadableModel) {
                loadableModel.reset();
            }

            model.detach();
            return;
        }

        //noinspection rawtypes
        if (delegateProvider instanceof MultivalueContainerListDataProvider multivalueProvider) {
            multivalueProvider.getModel().detach();
        }
    }

    public <T> void applyDefaultRowCss(
            @NotNull Component tile,
            @NotNull O value,
            @NotNull String baseCss,
            @NotNull SerializableFunction<O, @Nullable StatusInfo<T>> getStatusInfoFn) {
        PV columnValue = value.getColumnValue();
        if (columnValue instanceof PrismContainerValueWrapper<?> wrapper) {
            StatusInfo<T> statusInfo = getStatusInfoFn.apply(value);
            if (statusInfo != null && statusInfo.getStatus() != null) {
                SmartIntegrationUtils.SuggestionUiStyle uiStyle =
                        SmartIntegrationUtils.SuggestionUiStyle.from(statusInfo, wrapper);
                tile.add(AttributeModifier.append("class", baseCss + " border-large-left " + uiStyle.tileClass));
            }
        }
    }

    @NotNull
    protected AjaxIconButton createNewObjectPerformButton(String idButton) {
        AjaxIconButton newObjectButton = new AjaxIconButton(
                idButton,
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
        newObjectButton.add(AttributeAppender.replace("class", getNewObjectButtonCssClass()));
        newObjectButton.add(new VisibleBehaviour(this::isNewObjectCreationEnabled));
        return newObjectButton;
    }

    protected String getNewObjectButtonCssClass() {
        return "btn btn-outline-primary ml-auto";
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
        ButtonBar<Containerable, SelectableRow<?>> buttonBar =
                new ButtonBar<>(id, ID_BUTTON_BAR, ColumnTileTable.this, createToolbarButtonsList(ID_BUTTON));
        buttonBar.setOutputMarkupId(true);
        buttonBar.add(new VisibleBehaviour(this::isToolbarButtonsVisible));
        return buttonBar;
    }

    private @NotNull Component createPanelToolbar() {
        WebMarkupContainer panelToolbar = new WebMarkupContainer(ID_PANEL_TOOLBAR);
        panelToolbar.setOutputMarkupId(true);

        RepeatingView toolbar = new RepeatingView(ID_PANEL_TOOLBAR_BUTTONS);
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
        UserProfileStorage userProfile = getBrowserTabSessionStorage().getUserProfile();
        userProfile.setPagingSize(getTableId(), pageItemSize);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected IModel<Search> createSearchModel() {
        return super.createSearchModel();
    }

    @Override
    public boolean displayNoValuePanel() {
        return false;
    }

    @Override
    protected List<Component> createNoValueButtonToolbar(String id) {
        List<Component> buttons = new ArrayList<>();
        buttons.add(createNewObjectPerformButton(id));
        return buttons;
    }
}
