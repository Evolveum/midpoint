/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.isObjectSelected;
import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.setColumnTileSelected;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedRadioPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemBuilder;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * Group tile panel that renders multiple delegated values inside one logical tile.
 *
 * <p>The outer tile represents one grouped row object {@code O}, while each inner rendered row
 * is backed by one delegated value {@code PV}. This is useful when a single tile groups
 * multiple mappings but still wants to reuse the standard single-row column rendering.
 */
public class MappingSuggestionGroupColumnTilePanel<
        O extends ColumnValueProvider<PV>,
        PV extends Serializable,
        T extends ColumnTile<O, PV>>
        extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_COLUMNS_TILE_FRAGMENT = "columnTileFragment";
    private static final String ID_ROWS_CONTAINER = "rowsContainer";
    private static final String ID_ROWS = "rows";
    private static final String ID_ROW = "row";
    private static final String ID_TOOLBAR = "toolbar";
    private static final String ID_HEADER = "tile-column-header";
    private static final String ID_NOTE = "note";
    private static final String ID_NOTE_LABEL = "noteLabel";
    private static final String ID_COLUMN_HEADER = "columnHeader";

    private final IModel<PV> selectedRowModel = new Model<>();

    public MappingSuggestionGroupColumnTilePanel(String id, IModel<T> model) {
        super(id, model);
    }

    protected boolean isInbound() {
        return true;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        add(AttributeAppender.replace("class", getPanelCss()));
        add(createContentFragment());
    }

    private @NotNull Component createContentFragment() {
        Fragment fragment = new Fragment(ID_CONTENT_CONTAINER, ID_COLUMNS_TILE_FRAGMENT, this);
        fragment.setOutputMarkupId(true);

        List<PV> columnValues = getColumnValues();
        initSelectedRowModel(columnValues);

        fragment.add(createHeaderContainer());
        fragment.add(createRowsContainer(columnValues));

        initSeparator(fragment, "separator");
        initHeaderTitle(fragment);
        initSeparator(fragment, "separatorSecond");
        initTargetRef(fragment);
        initShowSuggestionLink(fragment);
        initAcceptSelectedButton(fragment);
        initActions(fragment);

        return fragment;
    }

    private void initSeparator(@NotNull Fragment fragment, String id) {
        WebMarkupContainer separator = new WebMarkupContainer(id);
        separator.add(new VisibleBehaviour(this::isInbound));
        fragment.add(separator);
    }

    private @NotNull List<PV> getColumnValues() {
        O value = getModelObject().getValue();
        return value != null && value.getColumnsValues() != null
                ? value.getColumnsValues()
                : List.of();
    }

    private void initSelectedRowModel(@NotNull List<PV> columnValues) {
        if (selectedRowModel.getObject() == null && !columnValues.isEmpty()) {
            selectedRowModel.setObject(columnValues.get(0));
        }
    }

    private @NotNull WebMarkupContainer createHeaderContainer() {
        WebMarkupContainer headerContainer = new WebMarkupContainer(ID_HEADER);
        headerContainer.setOutputMarkupId(true);

        RepeatingView toolbar = new RepeatingView(ID_TOOLBAR);
        headerContainer.add(toolbar);
        addToolbarButtons(toolbar);

        return headerContainer;
    }

    private @NotNull WebMarkupContainer createRowsContainer(@NotNull List<PV> columnValues) {
        WebMarkupContainer rowsContainer = new WebMarkupContainer(ID_ROWS_CONTAINER);
        rowsContainer.setOutputMarkupId(true);
        rowsContainer.setOutputMarkupPlaceholderTag(true);
        rowsContainer.add(new VisibleBehaviour(() -> getModelValue().isExpanded()));

        rowsContainer.add(createColumnHeaderPanel());
        rowsContainer.add(createRadioGroup(columnValues));
        rowsContainer.add(createNote());

        return rowsContainer;
    }

    private O getModelValue() {
        return getModelObject().getValue();
    }

    private @NotNull ColumnTileHeaderPanel<O, PV> createColumnHeaderPanel() {
        ColumnTileHeaderPanel<O, PV> columnHeaderPanel =
                new ColumnTileHeaderPanel<>(ID_COLUMN_HEADER, () -> getModelObject().getColumns(), false) {
                    @Override
                    protected void hideSpecificHeaderIfNeeded(IColumn<PV, String> column, WebMarkupContainer header) {
                        if (column instanceof InlineMenuButtonColumn<PV>) {
                            header.setVisible(false);
                        }
                    }
                };
        columnHeaderPanel.setOutputMarkupId(true);
        columnHeaderPanel.add(new VisibleBehaviour(this::isColumnHeadersVisible));
        return columnHeaderPanel;
    }

    protected boolean isColumnHeadersVisible() {
        return false;
    }

    private @NotNull RadioGroup<PV> createRadioGroup(@NotNull List<PV> columnValues) {
        RadioGroup<PV> radioGroup = new RadioGroup<>("radioGroup", selectedRowModel);
        radioGroup.setOutputMarkupId(true);
        radioGroup.add(createRadioGroupUpdateBehavior());
        radioGroup.add(createRowsView(columnValues));
        return radioGroup;
    }

    private @NotNull AjaxFormChoiceComponentUpdatingBehavior createRadioGroupUpdateBehavior() {
        return new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                PV selected = selectedRowModel.getObject();
                if (selected != null) {
                    onRowSelected(selected, target);
                }
                target.add(MappingSuggestionGroupColumnTilePanel.this);
            }
        };
    }

    private @NotNull ListView<PV> createRowsView(@NotNull List<PV> columnValues) {
        return new ListView<>(ID_ROWS, columnValues) {
            @Override
            protected void populateItem(@NotNull ListItem<PV> rowItem) {
                PV rowValue = rowItem.getModelObject();
                if (rowValue == null) {
                    rowItem.setVisible(false);
                    return;
                }

                WebMarkupContainer rowWrapper = createRowWrapper();
                rowWrapper.add(createRowPanel(rowValue));

                rowItem.add(rowWrapper);
            }
        };
    }

    private @NotNull WebMarkupContainer createRowWrapper() {
        WebMarkupContainer rowWrapper = new WebMarkupContainer("rowWrapper");
        rowWrapper.add(AttributeAppender.append("class", getTileCssClasses()));
        rowWrapper.add(AttributeAppender.append("style", getTileCssStyle()));
        rowWrapper.setOutputMarkupPlaceholderTag(true);
        rowWrapper.setOutputMarkupId(true);
        return rowWrapper;
    }

    private @NotNull ColumnTilePanel<O, PV, T> createRowPanel(@NotNull PV rowValue) {
        IModel<T> tileModel = getModel();

        ColumnTilePanel<O, PV, T> rowPanel = new ColumnTilePanel<>(ID_ROW, tileModel, Model.of(rowValue)) {
            @Override
            protected boolean isInlineMenuButtonVisible() {
                return false;
            }

            @Override
            protected boolean isCheckboxVisible() {
                return false;
            }

            @Override
            protected @NotNull String getDefaultPanelCss() {
                String css = super.getDefaultPanelCss() + " " + getAdditionalDefaultTilePanelCss(rowValue);
                if (Objects.equals(selectedRowModel.getObject(), rowValue)) {
                    css += " selected-base";
                }

                return css;
            }

            @Override
            protected void addToolbarButtons(@NotNull RepeatingView repeatingView) {
                super.addToolbarButtons(repeatingView);
                repeatingView.add(createRowRadioPanel(repeatingView, rowValue));
            }
        };

        rowPanel.setOutputMarkupId(true);
        return rowPanel;
    }

    private @NotNull IsolatedRadioPanel<PV> createRowRadioPanel(
            @NotNull RepeatingView repeatingView, @NotNull PV rowValue) {
        return new IsolatedRadioPanel<>(repeatingView.newChildId(), () -> rowValue, Model.of(true));
    }

    private @NotNull WebMarkupContainer createNote() {
        WebMarkupContainer note = new WebMarkupContainer(ID_NOTE);
        note.setOutputMarkupPlaceholderTag(true);

        Label noteLabel = new Label(
                ID_NOTE_LABEL,
                createStringResource("MappingSuggestionGroupColumnTilePanel.note",
                        getModelObject().getValue().getKeyValue()));
        noteLabel.setEscapeModelStrings(false);
        note.add(noteLabel);

        return note;
    }

    protected void onRowSelected(@NotNull PV rowValue, @NotNull AjaxRequestTarget target) {
        O value = getModelObject().getValue();
        if (value == null || value.getColumnsValues() == null) {
            return;
        }

        for (PV candidate : value.getColumnsValues()) {
            if (candidate instanceof PrismContainerValueWrapper<?> wrapper) {
                wrapper.setSelected(Objects.equals(candidate, rowValue));
            }
        }
    }

    protected void addToolbarButtons(@NotNull RepeatingView repeatingView) {
        initCheckBoxPanel(repeatingView);
    }

    private void initShowSuggestionLink(@NotNull WebMarkupContainer container) {
        Fragment fragment = createToolbarFragment("showSuggestionLink");

        AjaxIconButton button = new AjaxIconButton(
                "item",
                Model.of("fa-solid fa-magnifying-glass"),
                createStringResource("MappingSuggestionGroupColumnTilePanel.showSuggestion")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                getModelValue().setExpanded(!getModelValue().isExpanded());
                target.add(MappingSuggestionGroupColumnTilePanel.this);
            }
        };

        button.setOutputMarkupId(true);
        button.add(AttributeAppender.append("class", "ml-auto btn btn-link p-0"));
        button.showTitleAsLabel(true);

        fragment.add(button);
        fragment.add(new VisibleBehaviour(() -> !getModelValue().isExpanded()));
        container.add(fragment);
    }

    private void initAcceptSelectedButton(@NotNull WebMarkupContainer container) {
        Fragment fragment = createToolbarFragment("acceptSelectedButton");

        AjaxIconButton button = new AjaxIconButton(
                "item",
                Model.of("fa-solid fa-check"),
                createStringResource("MappingSuggestionGroupColumnTilePanel.acceptSelected")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PV selected = selectedRowModel.getObject();
                if (selected == null) {
                    return;
                }

                performOnAccept(target, selected);
            }
        };

        button.setOutputMarkupId(true);
        button.add(AttributeAppender.append("class", "ml-auto btn btn-outline-primary"));
        button.showTitleAsLabel(true);

        fragment.add(button);
        fragment.add(new VisibleBehaviour(() -> getModelValue().isExpanded()));
        container.add(fragment);
    }

    protected void performOnAccept(AjaxRequestTarget target, PV selected) {
        onAcceptSelected(selected, target);
    }

    protected InlineMenuItem createDeleteSelectedItemMenu(IModel<PV> selectedRowModel) {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-trash text-danger")
                .additionalCssClass("text-danger")
                .label(createStringResource("MappingSuggestionGroupColumnTilePanel.deleteSelected"))
                .action(new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        MappingSuggestionGroupColumnTilePanel.this.onDeletePerform(selectedRowModel, target);
                        refresh(target);
                    }
                })
                .buildInlineMenu();
    }

    protected InlineMenuItem createDeleteGroupItemMenu(O group) {
        return InlineMenuItemBuilder.create()
                .icon("fa fa-x text-danger")
                .additionalCssClass("text-danger")
                .label(createStringResource("MappingSuggestionGroupColumnTilePanel.deleteGroup"))
                .action(new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        group.getColumnsValues().forEach(value -> {
                            IModel<PV> model = Model.of(value);
                            MappingSuggestionGroupColumnTilePanel.this.onDeletePerform(model, target);
                        });
                        refresh(target);
                    }
                })
                .buildInlineMenu();
    }

    protected void onDeletePerform(IModel<PV> selectedRowModel, AjaxRequestTarget target) {

    }

    protected void refresh(AjaxRequestTarget target) {
    }

    private void initActions(@NotNull WebMarkupContainer container) {
        Fragment fragment = createToolbarFragment("actions");

        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(createDeleteSelectedItemMenu(selectedRowModel));
        menuItems.add(createDeleteGroupItemMenu(getModelObject().getValue()));

        DropdownButtonPanel inlineMenu = new DropdownButtonPanel(
                "item",
                new DropdownButtonDto(null, "fa fa-ellipsis-h", null, menuItems)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected @NotNull String getSpecialButtonClass() {
                return "btn btn-link btn-sm";
            }

            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Override
            protected boolean showIcon() {
                return true;
            }
        };

        fragment.add(inlineMenu);
        fragment.add(new VisibleBehaviour(() -> getModelValue().isExpanded()));
        container.add(fragment);
    }

    private void initHeaderTitle(@NotNull WebMarkupContainer container) {
        Fragment fragment = new Fragment("headerTitle", "headerTitleFragment", this);

        AjaxIconButton button = buildExpandableHeaderTitle("text");
        button.add(AttributeAppender.append("class", " btn p-0 text-dark"));
        fragment.add(button);

        Label label = new Label("badge", getModelObject().getValue().getCount());
        label.add(AttributeAppender.append("style", "height: fit-content;"));
        fragment.add(label);

        Label targetText = new Label("targetText", Model.of(getModelObject().getValue().getKeyValue()));
        fragment.add(targetText);

        container.add(fragment);
    }

    private @NotNull AjaxIconButton buildExpandableHeaderTitle(String text) {
        AjaxIconButton button = new AjaxIconButton(text, () -> getModelValue().isExpanded()
                ? "mr-3 fa-solid fa-chevron-up"
                : "mr-3 fa-solid fa-chevron-down", createStringResource("MappingSuggestionGroupColumnTilePanel.headerTitle")) {

            @Override
            public void onClick(@NotNull AjaxRequestTarget ajaxRequestTarget) {
                getModelValue().setExpanded(!getModelValue().isExpanded());
                ajaxRequestTarget.add(MappingSuggestionGroupColumnTilePanel.this);
            }
        };
        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        return button;
    }

    private void initTargetRef(@NotNull WebMarkupContainer container) {
        Fragment fragment = createToolbarFragment("targetRef");

        Label label = new Label("item", getModelObject().getValue().getKeyValue());
        fragment.add(label);
        container.add(fragment);
    }

    private @NotNull Fragment createToolbarFragment(@NotNull String id) {
        return new Fragment(id, "toolbarFragment", this);
    }

    private void initCheckBoxPanel(@NotNull RepeatingView repeatingView) {
        IModel<Boolean> selectedModel = new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                return isObjectSelected(getModelObject().getValue());
            }

            @Override
            public void setObject(Boolean value) {
                setColumnTileSelected(getModelObject().getValue(), Boolean.TRUE.equals(value));
            }
        };

        IsolatedCheckBoxPanel checkBox = new IsolatedCheckBoxPanel(repeatingView.newChildId(), selectedModel) {
            @Override
            public void onUpdate(@NotNull AjaxRequestTarget target) {
                Component component = MappingSuggestionGroupColumnTilePanel.this.findParent(ColumnTileTable.class);
                target.add(Objects.requireNonNullElse(component, MappingSuggestionGroupColumnTilePanel.this));

                component = MappingSuggestionGroupColumnTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, MappingSuggestionGroupColumnTilePanel.this));
            }
        };

        checkBox.setOutputMarkupId(true);
        repeatingView.add(checkBox);
    }

    protected @NotNull String getPanelCss() {
        String baseCss = "card col-12 m-0 border border-ai-gradient border-start-2 p-0";
        boolean selected = isObjectSelected(getModelObject().getValue());
        return selected ? baseCss + " selected-base" : baseCss;
    }

    protected String getTileCssStyle() {
        return "min-height: 50px;";
    }

    protected String getTileCssClasses() {
        return "d-flex col-12 h-100 justify-content-center mb-2 p-0 bg-white rounded";
    }

    protected String getTileContainerCssClass() {
        return "d-flex flex-wrap justify-content-start pt-2";
    }

    public String getAdditionalDefaultTilePanelCss(PV rowValue) {
        return "";
    }

    protected void onAcceptSelected(@NotNull PV selected, @NotNull AjaxRequestTarget target) {
    }

}
