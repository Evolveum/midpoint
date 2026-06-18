/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button.FocusStatisticsButton;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button.ObjectClassStatisticsButton;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.button.ObjectTypeStatisticsButton;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabSeparatedTabbedPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeValueCountType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeValuePatternCountType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.danekja.java.util.function.serializable.SerializableToIntFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.applyStaticPopupBackdrop;
import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.restoreBackdropPopupDefaults;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

/**
 * Popup panel that displays computed statistics for a resource object class, object type and focus type
 * (depending on the context of invocation). It includes a list of attributes with their respective statistics,
 * and for each selected attribute, it shows the most common values and value patterns.
 **/
public class SmartStatisticsPanel extends BasePanel<ShadowObjectClassStatisticsType> implements Popupable {

    private static final String ID_SEARCH_CONTAINER = "searchContainer";
    private static final String ID_SEARCH_INPUT = "searchInput";

    private static final String ID_CLOSE_BUTTON = "closeButton";
    private static final String ID_FOOTER_BUTTONS = "footerButtons";
    private static final String ID_SHOW_ALL_BUTTON = "showAllButton";

    private static final String ID_LEFT_PANEL = "leftPanelContainer";
    private static final String ID_CHIPS_CONTAINER = "chipsContainer";
    private static final String ID_TOTAL_OBJECTS = "totalObjects";
    private static final String ID_COVERAGE = "coverage";
    private static final String ID_SORT_TOOGLE = "sortToggle";
    private static final String ID_LIST_VIEW_CONTAINER = "listViewContainer";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_MAIN_PANEL_CONTAINER = "mainPanelContainer";
    private static final String ID_WIDGETS_CONTAINER = "widgetsContainer";

    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_TABLE_FRAGMENT = "tableFragment";
    private static final String ID_TABLE_PANEL_FRAGMENT = "table";

    private static final String ID_HEADER_FRAGMENT = "title";
    private static final String ID_HEADER_PRIMARY_TITLE = "primaryTitle";

    private static final String ID_TIMESTAMP = "timestamp";
    private static final String ID_RECREATE_BUTTON = "recreateButton";

    private IModel<String> searchModel = Model.of("");

    private final IModel<Boolean> isAllAttributesDisplayed = Model.of(false);

    private final String resourceOid;
    private final QName objectClassName;
    private final ResourceObjectTypeIdentification objectTypeIdentification;
    private final QName focusType;

    private IModel<ShadowAttributeStatisticsType> selectedAttribute;

    private final PanelType panelType;
    private boolean isFreqTableDisplayed = true;

    enum PanelType {
        OBJECT_CLASS, FOCUS, OBJECT_TYPE
    }

    private enum SortMode {
        COUNT, ALPHABETICAL
    }

    private final IModel<SortMode> sortModeModel = Model.of(SortMode.COUNT);

    public SmartStatisticsPanel(
            String id,
            IModel<ShadowObjectClassStatisticsType> model,
            String resourceOid,
            QName objectClassName) {
        super(id, model);
        this.resourceOid = resourceOid;
        this.objectClassName = objectClassName;
        this.objectTypeIdentification = null;
        this.focusType = null;
        this.panelType = PanelType.OBJECT_CLASS;
    }

    public SmartStatisticsPanel(
            String id,
            IModel<ShadowObjectClassStatisticsType> model,
            String resourceOid,
            ResourceObjectTypeIdentification objectTypeIdentification) {
        super(id, model);
        this.resourceOid = resourceOid;
        this.objectClassName = null;
        this.objectTypeIdentification = objectTypeIdentification;
        this.focusType = null;
        this.panelType = PanelType.OBJECT_TYPE;
    }

    public SmartStatisticsPanel(
            String id,
            IModel<ShadowObjectClassStatisticsType> model,
            String resourceOid,
            ResourceObjectTypeIdentification objectTypeIdentification,
            QName focusType) {
        super(id, model);
        this.resourceOid = resourceOid;
        this.objectClassName = null;
        this.objectTypeIdentification = objectTypeIdentification;
        this.focusType = focusType;
        this.panelType = PanelType.FOCUS;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initSearchModel();

        applyStaticPopupBackdrop(getPageBase());

        add(AttributeModifier.append("class", "p-0"));
        initSelectionModel();

        add(buildLeftPanel(getModelObject()));
        add(buildMainPanel().setOutputMarkupId(true));
    }

    private void initSearchModel() {
        searchModel = getDefaultSearchModel();
    }

    protected void initSelectionModel() {
        selectedAttribute = new LoadableModel<>() {
            @Override
            protected @NotNull ShadowAttributeStatisticsType load() {
                return new ShadowAttributeStatisticsType();
            }
        };

        ItemPathType defaultPath = getDefaultSelectedAttributePath();
        if (defaultPath != null) {
            ShadowAttributeStatisticsType def = findAttributeByPath(getModelObject(), defaultPath);
            if (def != null) {
                selectedAttribute.setObject(def);
            }
        }

        renderListViewRows(getModelObject());
    }

    protected List<ListViewRow> renderListViewRows(@NotNull ShadowObjectClassStatisticsType statistics) {
        Stream<ListViewRow> rows = statistics.getAttribute().stream()
                .map(item -> toAttributeRow(item, statistics));

        String normalizedSearch = normalizeSearch(searchModel.getObject());
        if (!normalizedSearch.isEmpty()) {
            rows = rows.filter(row -> matchesSearch(row, normalizedSearch));
        }

        List<ListViewRow> result = rows
                .sorted(createRowComparator())
                .peek(this::initInitialSelection)
                .collect(Collectors.toList());

        isAllAttributesDisplayed.setObject(
                result.size() == statistics.getAttribute().size()
        );
        return result;
    }

    private @NotNull Comparator<ListViewRow> createRowComparator() {
        Comparator<ListViewRow> alphabetical = Comparator.comparing(
                row -> extractSortableText(row.text),
                String.CASE_INSENSITIVE_ORDER);

        Comparator<ListViewRow> byCountDesc = Comparator.comparingInt(this::extractRowCount).reversed();

        if (sortModeModel.getObject() == SortMode.ALPHABETICAL) {
            return alphabetical.thenComparing(byCountDesc);
        }

        return byCountDesc.thenComparing(alphabetical);
    }

    private int extractRowCount(@NotNull ListViewRow row) {
        return extractCount(row.subText != null ? row.subText.getObject() : null);
    }

    private @NotNull String extractSortableText(IModel<String> model) {
        return model == null || model.getObject() == null ? "" : model.getObject();
    }

    private @NotNull String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(@NotNull ListViewRow row, @NotNull String normalized) {
        String text = row.text != null ? row.text.getObject() : null;
        String subText = row.subText != null ? row.subText.getObject() : null;

        return containsIgnoreCase(text, normalized) || containsIgnoreCase(subText, normalized);
    }

    private boolean containsIgnoreCase(String value, String normalizedNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedNeedle);
    }

    private @NotNull ListViewRow toAttributeRow(
            @NotNull ShadowAttributeStatisticsType item,
            @NotNull ShadowObjectClassStatisticsType statistics) {
        String refStr = item.getRef().getItemPath().lastName().toString();
        int count = statistics.getSize() - item.getMissingValueCount();

        return new ListViewRow(Model.of(refStr), Model.of(count + " values"), item);
    }

    public record ListViewRow(
            IModel<String> text,
            IModel<String> subText,
            ShadowAttributeStatisticsType item) implements Serializable {
    }

    private @NotNull WebMarkupContainer buildLeftPanel(ShadowObjectClassStatisticsType statistics) {
        WebMarkupContainer left = new WebMarkupContainer(ID_LEFT_PANEL);
        left.setOutputMarkupId(true);

        left.add(buildChips(statistics));
        left.add(buildSearch());
        left.add(buildListView(statistics, selectedAttribute));

        AjaxIconButton showAllButton = new AjaxIconButton(
                ID_SHOW_ALL_BUTTON,
                Model.of("fa fa-eye"),
                createStringResource("SmartStatisticsPanel.showAll")) {

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                searchModel.setObject("");

                target.add(getSearchContainer());
                target.add(getListViewContainer());
                target.add(this);
            }
        };

        showAllButton.setOutputMarkupId(true);
        showAllButton.setOutputMarkupPlaceholderTag(true);
        showAllButton.showTitleAsLabel(true);
        showAllButton.add(new VisibleBehaviour(() -> !isAllAttributesDisplayed.getObject()));
        left.add(showAllButton);

        return left;
    }

    protected WebMarkupContainer getSearchContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_LEFT_PANEL, ID_SEARCH_CONTAINER));
    }

    private @NotNull Component buildSearch() {
        Fragment fragment = new Fragment(ID_SEARCH_CONTAINER, ID_SEARCH_CONTAINER, this);

        TextField<String> search = new TextField<>(ID_SEARCH_INPUT, searchModel);
        search.setOutputMarkupId(true);
        search.add(AttributeModifier.append(
                "onkeydown",
                "if (event.key === 'Enter') { event.preventDefault(); return false; }"));
        search.add(new AjaxFormComponentUpdatingBehavior("input") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getListViewContainer());
                target.add(getShowAllButton());
            }
        });
        fragment.add(search);

        AjaxIconButton sortToggle = buildSortToggleButton();
        fragment.add(sortToggle);
        fragment.setOutputMarkupId(true);

        return fragment;
    }

    private @NotNull AjaxIconButton buildSortToggleButton() {
        AjaxIconButton sortToggle = new AjaxIconButton(
                ID_SORT_TOOGLE,
                () -> sortModeModel.getObject() == SortMode.COUNT
                        ? "fa fa-sort-amount-desc"
                        : "fa fa-sort-alpha-down",
                Model.of()) {

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                SortMode current = sortModeModel.getObject();
                sortModeModel.setObject(
                        current == SortMode.COUNT ? SortMode.ALPHABETICAL : SortMode.COUNT);

                target.add(getListViewContainer(), this);
            }
        };

        sortToggle.setOutputMarkupId(true);
        sortToggle.add(new TooltipBehavior());
        sortToggle.add(AttributeModifier.append("title", () -> sortModeModel.getObject().equals(SortMode.COUNT)
                ? createStringResource("SmartStatisticsPanel.sortByCount").getObject()
                : createStringResource("SmartStatisticsPanel.sortAlphabetically").getObject()));
        return sortToggle;
    }

    private Component getShowAllButton() {
        return get(ID_LEFT_PANEL).get(ID_SHOW_ALL_BUTTON);
    }

    private @NotNull WebMarkupContainer buildChips(@NotNull ShadowObjectClassStatisticsType statistics) {
        WebMarkupContainer chips = new WebMarkupContainer(ID_CHIPS_CONTAINER);
        chips.setOutputMarkupId(true);

        chips.add(createMetricPanel(
                ID_TOTAL_OBJECTS,
                createStringResource("SmartStatisticsPanel.totalObjects"),
                createStringResource(
                        "SmartStatisticsPanel.totalObjects.value",
                        statistics.getSize(),
                        statistics.getSize()),
                createStringResource("SmartStatisticsPanel.totalObjects.help"),
                Model.of("fa fa-cube fa-2x text-primary")));

        chips.add(createMetricPanel(
                ID_COVERAGE,
                createStringResource("SmartStatisticsPanel.coverage"),
                () -> String.valueOf(statistics.getAttribute().size()),
                createStringResource("SmartStatisticsPanel.coverage.help"),
                Model.of("fa fa-adjust fa-2x text-primary")));

        return chips;
    }

    private @NotNull WebMarkupContainer buildListView(
            ShadowObjectClassStatisticsType statistics,
            @NotNull IModel<ShadowAttributeStatisticsType> selectedAttribute) {

        WebMarkupContainer container = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        container.setOutputMarkupId(true);

        ListView<ListViewRow> listView =
                new ListView<>(ID_LIST_VIEW, () -> renderListViewRows(statistics)) {

                    @Override
                    protected void populateItem(@NotNull ListItem<ListViewRow> item) {
                        ListViewRow row = item.getModelObject();

                        item.add(new Label(ID_TEXT, row.text));
                        item.add(new Label(ID_SUBTEXT, row.subText));

                        if (row.item.equals(selectedAttribute.getObject())) {
                            item.add(AttributeModifier.append(CLASS_CSS, "cursor-pointer border-primary"));
                        }

                        item.add(new AjaxEventBehavior("click") {
                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                selectedAttribute.setObject(row.item);

                                target.add(getListViewContainer());
                                refreshMainPanel(target);
                            }
                        });
                    }
                };

        listView.setOutputMarkupId(true);
        container.add(listView);

        return container;
    }

    private @NotNull WebMarkupContainer buildWidgetsContainer(
            int totalValues,
            int uniqueValues,
            int missingValues) {

        RepeatingView widgets = new RepeatingView(ID_WIDGETS_CONTAINER);
        widgets.setOutputMarkupId(true);

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.totalValues"),
                () -> String.valueOf(totalValues),
                Model.of(),
                Model.of("")));

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.uniqueValues"),
                () -> String.valueOf(uniqueValues),
                Model.of(),
                Model.of("")));

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.missingValues"),
                () -> String.valueOf(missingValues),
                Model.of(),
                Model.of("")));

        return widgets;
    }

    private @NotNull WebMarkupContainer buildMainPanel() {
        WebMarkupContainer main = new WebMarkupContainer(ID_MAIN_PANEL_CONTAINER);
        main.setOutputMarkupId(true);

        int total = 0;
        int unique = 0;
        int missing = 0;

        ShadowAttributeStatisticsType object = selectedAttribute.getObject();
        if (object != null && object.getValueCount() != null) {
            total = getModelObject().getSize() - object.getMissingValueCount();
            missing = object.getMissingValueCount();
            unique = object.getUniqueValueCount();
        }

        main.addOrReplace(buildWidgetsContainer(total, unique, missing));

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createFrequencyTableTab(total));
        tabs.add(createPatternTableTab());

        TabSeparatedTabbedPanel<ITab> tabPanel = new TabSeparatedTabbedPanel<>(ID_TAB_PANEL, tabs) {

            @Override
            protected WebMarkupContainer newTabsContainer(String id) {
                WebMarkupContainer components = super.newTabsContainer(id);
                components.add(AttributeModifier.append(CLASS_CSS, "border-start border-end rounded-top bg-light"));
                return components;
            }

            @Override
            protected void onClickTabPerformed(int index, @NotNull Optional<AjaxRequestTarget> target) {
                isFreqTableDisplayed = index == 0;
                super.onClickTabPerformed(index, target);
            }

            @Override
            protected void customizePopulatedLoopItem(LoopItem item, int index, ITab tab, @NotNull WebMarkupContainer titleLink) {
                titleLink.add(new TooltipBehavior());
                titleLink.add(AttributeModifier.append("title", getHelpForTab(index)));
            }
        };

        switchTabs(tabPanel);

        tabPanel.setOutputMarkupId(true);
        main.add(tabPanel);

        return main;
    }

    protected String getHelpForTab(int index) {
        if (index == 0) {
            return createStringResource("SmartStatisticsPanel.valueFrequency.help").getObject();
        }
        return createStringResource("SmartStatisticsPanel.valuePatterns.help").getObject();
    }

    private void switchTabs(TabSeparatedTabbedPanel<ITab> tabPanel) {
        if (isFreqTableDisplayed) {
            tabPanel.setSelectedTab(0);
        } else {
            tabPanel.setSelectedTab(1);
        }
    }

    private @NotNull ITab createPatternTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "SmartStatisticsPanel.valuePatterns.header")) {
            @Override
            public WebMarkupContainer getPanel() {
                return super.getPanel();
            }

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return buildPatternTable(panelId, selectedAttribute.getObject());
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-project-diagram");
            }
        };
    }

    private @NotNull ITab createFrequencyTableTab(int total) {
        return new IconPanelTab(
                getPageBase().createStringResource(
                        "SmartStatisticsPanel.valueFrequency.header")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return buildFrequencyTable(panelId, selectedAttribute.getObject(), total);
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-chart-bar");
            }
        };
    }

    private @NotNull Fragment buildPatternTable(String id, @NotNull ShadowAttributeStatisticsType stat) {
        List<ShadowAttributeValuePatternCountType> valuePatternCount = stat.getValuePatternCount();

        List<IColumn<ShadowAttributeValuePatternCountType, String>> cols = List.of(
                badgeColumn(r -> r.getType().value()),
                new PropertyColumn<>(
                        createStringResource("SmartStatisticsPanel.valuePatterns.value"),
                        ShadowAttributeValuePatternCountType.F_VALUE.getLocalPart(),
                        "value"),
                new PropertyColumn<>(
                        createStringResource("SmartStatisticsPanel.valuePatterns.count"),
                        ShadowAttributeValuePatternCountType.F_COUNT.getLocalPart(),
                        "count")
        );

        return tableFragment(
                id,
                "SmartStatisticsPanel.valuePatterns.header",
                cols,
                new ListDataProvider<>(this, () -> valuePatternCount),
                createStringResource("SmartStatisticsPanel.valuePatterns.help"));
    }

    private @NotNull Fragment buildFrequencyTable(
            String panelId,
            @NotNull ShadowAttributeStatisticsType stat,
            int total) {

        List<IColumn<ShadowAttributeValueCountType, String>> cols = List.of(
                new PropertyColumn<>(
                        createStringResource("SmartStatisticsPanel.valueFrequency.value"),
                        ShadowAttributeValueCountType.F_VALUE.getLocalPart(),
                        "value"),
                new PropertyColumn<>(
                        createStringResource("SmartStatisticsPanel.valuePatterns.count"),
                        ShadowAttributeValueCountType.F_COUNT.getLocalPart(),
                        "count"),
                percentageColumn(total, ShadowAttributeValueCountType::getCount)
        );

        return tableFragment(
                panelId,
                "SmartStatisticsPanel.valueFrequency.header",
                cols,
                new ListDataProvider<>(this, stat::getValueCount),
                createStringResource("SmartStatisticsPanel.valueFrequency.help"));
    }

    private <R> @NotNull AbstractColumn<R, String> badgeColumn(
            SerializableFunction<R, String> getter) {

        return new AbstractColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.type")) {
            @Override
            public void populateItem(Item<ICellPopulator<R>> cell, String cid, IModel<R> row) {
                String type = getter.apply(row.getObject());
                String css = getBadgeTypeCss(type);

                cell.add(new Label(cid, type)
                        .add(AttributeModifier.replace("class", css)));
            }
        };
    }

    protected String getBadgeTypeCss(@NotNull String type) {
        if (type.equals("prefix") || type.equals("firstToken")) {
            return "badge bg-info px-2 py-1";
        } else if (type.equals("suffix") || type.equals("lastToken")) {
            return "badge bg-success px-2 py-1";
        }

        return "badge bg-secondary px-2 py-1";
    }

    private <R> @NotNull AbstractColumn<R, String> percentageColumn(
            int total,
            SerializableToIntFunction<R> countFn) {

        return new AbstractColumn<>(createStringResource("SmartStatisticsPanel.valueFrequency.percentage")) {
            @Override
            public void populateItem(Item<ICellPopulator<R>> cell, String cid, IModel<R> row) {
                int count = countFn.applyAsInt(row.getObject());
                String txt = total == 0 ? "0%" : String.format("%.2f%%", count * 100.0 / total);

                cell.add(new Label(cid, txt));
            }
        };
    }

    private <R extends Serializable> @NotNull Fragment tableFragment(
            String id,
            String headerKey,
            List<IColumn<R, String>> cols,
            ListDataProvider<R> provider,
            IModel<String> helpModel) {

        Fragment frag = new Fragment(id, ID_TABLE_FRAGMENT, this);
        BoxedTablePanel<R> table = new BoxedTablePanel<>(ID_TABLE_PANEL_FRAGMENT, provider, cols) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            public boolean displayIsolatedNoValuePanel() {
                return provider.size() == 0;
            }

            @Override
            protected StringResourceModel getNoValuePanelCustomSubTitleModel() {
                return createStringResource("SmartStatisticsPanel.noValuePanel.customSubTitle");
            }
        };

        table.setShowAsCard(false);
        table.setItemsPerPage(10);
        frag.add(table);

        return frag;
    }

    private @NotNull MetricValuePanel createMetricPanel(
            String id,
            IModel<String> title,
            IModel<String> valueCount,
            IModel<String> helpText,
            IModel<String> iconCss) {

        MetricValuePanel panel = new MetricValuePanel(id) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                IconWithLabel components = new IconWithLabel(id, title) {
                    @Override
                    protected String getIconCssClass() {
                        return iconCss.getObject();
                    }

                    @Override
                    protected String getLabelComponentCssClass() {
                        return "txt-toned";
                    }

                    @Override
                    protected String getComponentCssClass() {
                        return super.getComponentCssClass() + "mb-1 gap-2";
                    }
                };

                components.add(new TooltipBehavior());
                components.add(AttributeModifier.append("title", helpText.getObject()));

                return components;
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, valueCount);
                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-4 m-0 lh-1"));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size:20px"));

                return label;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getContainerCssClass() {
                return iconCss.getObject().isEmpty() ? "" : super.getContainerCssClass();
            }
        };

        panel.setOutputMarkupId(true);

        return panel;
    }

    protected void noPerformed(AjaxRequestTarget target) {
        restoreBackdropPopupDefaults(getPageBase());
        getPageBase().hideMainPopup(target);
    }

    protected StringResourceModel getCloseButtonModel() {
        return createStringResource("CardWithTablePanel.close");
    }

    protected boolean isCloseButtonVisible() {
        return true;
    }

    @Override
    public @NotNull Component getFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_FOOTER_BUTTONS, this);
        initCloseButton(footer);

        StringResourceModel timestampLabel = null;
        XMLGregorianCalendar timestamp = getModelObject().getTimestamp();
        if (timestamp != null) {
            timestampLabel = createStringResource(
                    "SmartStatisticsPanel.title.timestamped",
                    timestamp.toXMLFormat());
        }

        footer.add(new Label(ID_TIMESTAMP, timestampLabel).setVisible(timestampLabel != null));

        if (panelType == PanelType.FOCUS) {
            footer.add(buildFocusStatisticsButton());
        } else if (panelType == PanelType.OBJECT_CLASS) {
            footer.add(buildObjectClassStatisticsButton());
        } else {
            footer.add(buildObjectTypeStatisticsButton());
        }

        footer.add(AttributeAppender.append(CLASS_CSS, "flex-grow-1"));

        return footer;
    }

    private void initCloseButton(@NotNull Fragment footer) {
        AjaxLinkPanel closeButton = new AjaxLinkPanel(ID_CLOSE_BUTTON, getCloseButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        closeButton.setOutputMarkupId(true);
        closeButton.add(new VisibleBehaviour(this::isCloseButtonVisible));
        footer.add(closeButton);
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getHeightUnit() {
        return "vh";
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public IModel<String> getTitle() {
        if (panelType == PanelType.FOCUS) {
            return createStringResource("SmartStatisticsPanel.title.focus", focusType.getLocalPart());
        } else if (panelType == PanelType.OBJECT_TYPE) {
            return createStringResource(
                    "SmartStatisticsPanel.title.objectType",
                    objectTypeIdentification != null
                            ? objectTypeIdentification.getKind() + "/" + objectTypeIdentification.getIntent()
                            : "");
        } else if (panelType == PanelType.OBJECT_CLASS) {
            return createStringResource(
                    "SmartStatisticsPanel.title.objectClass",
                    objectClassName.getLocalPart());
        }

        return createStringResource("SmartStatisticsPanel.title");
    }

    @Override
    public @Nullable Component getTitleComponent() {
        return createHeaderFragment();
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa-solid fa-chart-line");
    }

    private @NotNull Fragment createHeaderFragment() {
        Fragment header = new Fragment(ID_HEADER_FRAGMENT, ID_HEADER_FRAGMENT, this);
        header.add(new Label(ID_HEADER_PRIMARY_TITLE, getTitle()));

        AjaxLink<Void> closeButton = new AjaxLink<>(ID_CLOSE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        closeButton.setOutputMarkupId(true);
        header.add(closeButton);

        header.add(AttributeAppender.append(CLASS_CSS, "flex-grow-1 mt-1"));

        return header;
    }

    private @NotNull ObjectTypeStatisticsButton buildObjectTypeStatisticsButton() {
        ObjectTypeStatisticsButton statisticsButton =
                new ObjectTypeStatisticsButton(ID_RECREATE_BUTTON, () -> objectTypeIdentification, resourceOid) {
                    @Override
                    protected boolean forceRegeneration() {
                        return true;
                    }

                    @Override
                    protected boolean isRegenerateMode() {
                        return true;
                    }
                };

        statisticsButton.setOutputMarkupId(true);

        return statisticsButton;
    }

    private @NotNull ObjectClassStatisticsButton buildObjectClassStatisticsButton() {
        ObjectClassStatisticsButton statisticsButton =
                new ObjectClassStatisticsButton(ID_RECREATE_BUTTON, () -> objectClassName, resourceOid) {
                    @Override
                    protected boolean forceRegeneration() {
                        return true;
                    }

                    @Override
                    protected boolean isRegenerateMode() {
                        return true;
                    }
                };

        statisticsButton.setOutputMarkupId(true);

        return statisticsButton;
    }

    protected FocusStatisticsButton buildFocusStatisticsButton() {
        FocusStatisticsButton statisticsButton =
                new FocusStatisticsButton(
                        ID_RECREATE_BUTTON,
                        () -> focusType,
                        () -> resourceOid,
                        objectTypeIdentification::getKind,
                        objectTypeIdentification::getIntent) {

                    @Override
                    protected boolean forceRegeneration() {
                        return true;
                    }

                    @Override
                    protected boolean isRegenerateMode() {
                        return true;
                    }
                };

        statisticsButton.setOutputMarkupId(true);

        return statisticsButton;
    }

    protected WebMarkupContainer getListViewContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_LEFT_PANEL, ID_LIST_VIEW_CONTAINER));
    }

    protected WebMarkupContainer getMainPanelContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_MAIN_PANEL_CONTAINER));
    }

    private void refreshMainPanel(@NotNull AjaxRequestTarget target) {
        WebMarkupContainer newMainPanel = buildMainPanel();
        getMainPanelContainer().replaceWith(newMainPanel);
        target.add(newMainPanel);
    }

    private int extractCount(String text) {
        if (text == null) {
            return 0;
        }

        String digits = text.replaceAll("\\D+", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private void initInitialSelection(@NotNull ListViewRow row) {
        if (selectedAttribute.getObject().getRef() == null) {
            selectedAttribute.setObject(row.item);
        }
    }

    @Nullable
    private ShadowAttributeStatisticsType findAttributeByPath(
            @NotNull ShadowObjectClassStatisticsType statistics,
            @NotNull ItemPathType path) {

        return statistics.getAttribute().stream()
                .filter(a -> a.getRef() != null
                        && a.getRef().getItemPath().endsWith(path.getItemPath()))
                .findFirst()
                .orElse(null);
    }

    protected ItemPathType getDefaultSelectedAttributePath() {
        return null;
    }

    public IModel<String> getDefaultSearchModel() {
        ItemPathType defaultSelectedAttributePath = getDefaultSelectedAttributePath();
        if (defaultSelectedAttributePath != null) {
            return Model.of(defaultSelectedAttributePath.getItemPath().lastName().toString());
        }
        return Model.of("");
    }
}
