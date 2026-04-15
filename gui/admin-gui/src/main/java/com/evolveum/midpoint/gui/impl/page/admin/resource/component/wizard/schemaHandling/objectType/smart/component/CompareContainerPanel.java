/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.io.Serial;
import java.util.*;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;

/**
 * A reusable popup panel that displays a side-by-side comparison of multiple
 * container objects based on selected attributes and items. It allows users
 * to dynamically choose which objects and attributes to compare.
 */
public class CompareContainerPanel<C extends Containerable> extends BasePanel<CompareObjectDto<C>> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABLE_CONTAINER = "tableContainer";
    private static final String ID_TABLE = "table";
    private static final String ID_COMPARE_ITEM_SELECTOR = "compareItemSelector";
    private static final String ID_COMPARE_PATH_SELECTOR = "comparePathSelector";
    private static final String ID_ACTION_BUTTONS = "actionButtons";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private IModel<List<CompareObjectDto.ComparedItem<C>>> selectedItemsModel;
    private IModel<List<ItemPath>> selectedPathsModel;

    public CompareContainerPanel(String id, IModel<CompareObjectDto<C>> dtoModel) {
        super(id, dtoModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        CompareObjectDto<C> modelObject = getModelObject();
        selectedItemsModel = Model.ofList(new ArrayList<>(modelObject.getComparedItems()));
        selectedPathsModel = Model.ofList(new ArrayList<>(modelObject.getComparedPaths()));

        initCompareItemSelector(modelObject.getComparedItems());
        initComparePathSelector(modelObject.getComparedPaths());
        initMoreActionsPanel();
        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);
        add(tableContainer);

        BoxedTablePanel<ItemPath> boxedTablePanel = buildCompareTable();
        tableContainer.add(boxedTablePanel);
    }

    private void initMoreActionsPanel() {
        DropdownButtonPanel buttonPanel = new DropdownButtonPanel(ID_ACTION_BUTTONS, new DropdownButtonDto(
                null, "fa-ellipsis-h", null, buildMoreActionsMenuItems())) {

            @Override
            protected @NotNull String getSpecialButtonClass() {
                return "btn btn-default";
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
        buttonPanel.setOutputMarkupId(true);
        add(buttonPanel);
    }

    private @NotNull List<InlineMenuItem> buildMoreActionsMenuItems() {
        //TODO
        return new ArrayList<>();
    }

    private void initComparePathSelector(List<ItemPath> comparedPaths) {
        IChoiceRenderer<ItemPath> renderer = new IChoiceRenderer<>() {
            @Override
            public @Unmodifiable Object getDisplayValue(@NotNull ItemPath path) {
                return path.toString();
            }

            @Override
            public @NotNull String getIdValue(ItemPath path, int index) {
                return String.valueOf(index);
            }
        };

        ListMultipleChoicePanel<ItemPath> pathSelector =
                new ListMultipleChoicePanel<>(ID_COMPARE_PATH_SELECTOR, selectedPathsModel, Model.ofList(comparedPaths),
                        renderer, this::createDefaultOptions);

        pathSelector.setOutputMarkupId(true);
        pathSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                rebuildCompareTable(target);
            }
        });

        add(pathSelector);
    }

    private @NotNull Map<String, String> createDefaultOptions() {
        Map<String, String> map = new HashMap<>();
        map.put("buttonContainer", "<div class=\"dropdown\">");
        map.put("buttonClass", "btn btn-default");
        map.put("buttonTextAlignment", "left");
        map.put("nonSelectedText", getString("CompareContainerPanel.nonSelectedText"));
        map.put("allSelectedText", getString("CompareContainerPanel.allSelectedText"));
        map.put("nSelectedText", getString("CompareContainerPanel.nSelectedText"));
        map.put("numberDisplayed", "-1");
        return map;
    }

    private void initCompareItemSelector(List<CompareObjectDto.ComparedItem<C>> allItems) {
        ChoiceProvider<CompareObjectDto.ComparedItem<C>> choiceProvider = new ChoiceProvider<>() {

            @Override
            public @NotNull String getDisplayValue(@NotNull CompareObjectDto.ComparedItem<C> choice) {
                return choice.identifier();
            }

            @Override
            public @NotNull String getIdValue(@NotNull CompareObjectDto.ComparedItem<C> choice) {
                return choice.identifier();
            }

            @Override
            public void query(String term, int page, @NotNull Response<CompareObjectDto.ComparedItem<C>> response) {
                allItems.stream()
                        .filter(item -> term == null || item.identifier().toLowerCase().contains(term.toLowerCase()))
                        .forEach(response::add);
            }

            @Override
            public @NotNull @Unmodifiable Collection<CompareObjectDto.ComparedItem<C>> toChoices(Collection<String> ids) {
                return allItems.stream()
                        .filter(item -> ids.contains(item.identifier()))
                        .toList();
            }
        };

        Select2MultiChoicePanel<CompareObjectDto.ComparedItem<C>> select2MultiChoicePanel =
                new Select2MultiChoicePanel<>(ID_COMPARE_ITEM_SELECTOR, () -> selectedItemsModel.getObject(), choiceProvider,
                        0);

        select2MultiChoicePanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                rebuildCompareTable(target);
            }
        });

        add(select2MultiChoicePanel);
    }

    private @NotNull BoxedTablePanel<ItemPath> buildCompareTable() {
        ListDataProvider<ItemPath> tableProvider = new ListDataProvider<>(
                CompareContainerPanel.this,
                () -> selectedPathsModel.getObject());

        BoxedTablePanel<ItemPath> boxedTablePanel = new BoxedTablePanel<>(
                ID_TABLE, tableProvider, createColumns(selectedItemsModel)) {

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };
        boxedTablePanel.setOutputMarkupId(true);
        return boxedTablePanel;
    }

    private @NotNull List<IColumn<ItemPath, String>> createColumns(
            @NotNull IModel<List<CompareObjectDto.ComparedItem<C>>> comparedItemsModel) {
        List<IColumn<ItemPath, String>> columns = new ArrayList<>();
        List<CompareObjectDto.ComparedItem<C>> comparedItems = comparedItemsModel.getObject();

        CompareObjectDto.ComparedItem<C> primaryItem = getModelObject().getPrimaryItem();
        columns.add(new AbstractColumn<>(Model.of("")) {

            @Override
            public void populateItem(Item<ICellPopulator<ItemPath>> item, String id, IModel<ItemPath> iModel) {
                PrismContainerDefinition<C> definition = primaryItem.value().getDefinition();
                ItemPath object = iModel.getObject();
                ItemDefinition<?> itemDefinition = definition.findItemDefinition(iModel.getObject());
                String displayName = itemDefinition.getDisplayName();
                if (displayName == null) {
                    displayName = StringUtils.capitalize(itemDefinition.getItemName().getLocalPart());
                }

                // TODO should we handle segments differently? E.g. container names?
                item.add(object.getSegments().size() > 1
                        ? createLabelWithPathDetail(item, id, iModel, displayName)
                        : new Label(id, createStringResource(displayName)));
            }

            private @NotNull LabelWithHelpPanel createLabelWithPathDetail(
                    @NotNull Item<ICellPopulator<ItemPath>> item,
                    String id,
                    IModel<ItemPath> iModel,
                    String displayName) {
                LabelWithHelpPanel label = new LabelWithHelpPanel(id,
                        createStringResource(displayName)) {
                    @Override
                    protected @NotNull IModel<String> getHelpModel() {
                        return Model.of(iModel.getObject().toString());
                    }

                    @Override
                    protected @NotNull String getCustomIconClass() {
                        return "fa fa-solid fa-sitemap text-primary";
                    }

                    @Override
                    protected @NotNull String getButtonContainerAdditionalCssClass() {
                        return "px-0";
                    }
                };
                label.add(AttributeModifier.append("class",
                        "d-flex flex-row-reverse align-items-center justify-content-end"));
                return label;
            }

            @Override
            public String getCssClass() {
                return "compared-header-column border-right";
            }

            @Override
            public Component getHeader(String componentId) {
                return super.getHeader(componentId);
            }
        });

        columns.add(createComparedColumn(primaryItem));

        comparedItems.forEach(comparedItem -> columns.add(createComparedColumn(comparedItem)));

        return columns;
    }

    private @NotNull AbstractColumn<ItemPath, String> createComparedColumn(
            @NotNull CompareObjectDto.ComparedItem<C> comparedItem) {
        return new AbstractColumn<>(Model.of(comparedItem.identifier())) {
            @Override
            public void populateItem(Item<ICellPopulator<ItemPath>> item, String s, IModel<ItemPath> iModel) {
                populateObjectProperty(item, s, iModel, comparedItem);
            }

            @Override
            public String getCssClass() {
                return "compared-item-column border-right";
            }
        };
    }

    private static <C extends Containerable> void populateObjectProperty(
            @NotNull Item<ICellPopulator<ItemPath>> item,
            String id,
            @NotNull IModel<ItemPath> iModel,
            CompareObjectDto.@NotNull ComparedItem<C> comparedItem) {
        PrismContainerValueWrapper<C> value = comparedItem.value();
        setReadOnlyRecursively(value);
        PrismPropertyPanel<?> panel = new PrismPropertyPanel<>(id,
                PrismPropertyWrapperModel.fromContainerValueWrapper(() -> value, iModel.getObject()),
                null) {

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }
        };
        item.add(panel);
    }

    private WebMarkupContainer getTableContainer() {
        return (WebMarkupContainer) get(ID_TABLE_CONTAINER);
    }

    private void rebuildCompareTable(@NotNull AjaxRequestTarget target) {
        BoxedTablePanel<ItemPath> newTable = buildCompareTable();
        getTableContainer().replace(newTable);
        target.add(getTableContainer());
    }

    private @NotNull Fragment buildFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxButton noButton = new AjaxButton(ID_CLOSE, createStringResource("CompareObjectPanel.button.close")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().getMainPopup().close(target);
            }
        };
        footer.add(noButton);

        return footer;
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CompareObjectPanel.title",
                getModelObject().getPrimaryItem().identifier());
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa fa-code-branch");
    }

    @Override
    public @NotNull Component getFooter() {
        return buildFooter();
    }

    @Override
    public Component getContent() {
        return this;
    }

}
