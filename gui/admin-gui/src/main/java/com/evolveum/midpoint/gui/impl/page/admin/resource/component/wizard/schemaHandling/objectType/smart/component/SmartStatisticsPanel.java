/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.\
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
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

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

//TODO (this is initial implementation)
public class SmartStatisticsPanel extends BasePanel<ShadowObjectClassStatisticsType> implements Popupable {

    private static final String ID_CLOSE_BUTTON = "closeButton";
    private static final String ID_FOOTER_BUTTONS = "footerButtons";

    private static final String ID_LEFT_PANEL = "leftPanelContainer";
    private static final String ID_CHIPS_CONTAINER = "chipsContainer";
    private static final String ID_TOTAL_OBJECTS = "totalObjects";
    private static final String ID_COVERAGE = "coverage";
    private static final String ID_TOGGLE_PANEL = "togglePanel";
    private static final String ID_LIST_VIEW_CONTAINER = "listViewContainer";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_MAIN_PANEL_CONTAINER = "mainPanelContainer";
    private static final String ID_WIDGETS_CONTAINER = "widgetsContainer";
    private static final String ID_TABLE_PATTERN_CONTAINER = "tablePatternContainer";
    private static final String ID_TABLE_FREQ_CONTAINER = "tableFreqContainer";
    private static final String ID_TABLE_COMB_CONTAINER = "tableCombContainer";

    private static final String ID_TABLE_FRAGMENT = "tableFragment";
    private static final String ID_TABLE_HEADER_FRAGMENT = "tableHeader";
    private static final String ID_TABLE_PANEL_FRAGMENT = "table";

    private static final String ID_HEADER_FRAGMENT = "title";
    private static final String ID_HEADER_PRIMARY_TITLE = "primaryTitle";
    private static final String ID_HEADER_SECONDARY_TITLE = "secondaryTitle";

    private final String resourceOid;
    private final QName objectClassName;

    private boolean isAttributeTuple = false;

    private IModel<ShadowAttributeTupleStatisticsType> selectedTuple;
    private IModel<ShadowAttributeStatisticsType> selectedAttribute;

    public SmartStatisticsPanel(String id, IModel<ShadowObjectClassStatisticsType> model, String resourceOid, QName objectClassName) {
        super(id, model);
        this.resourceOid = resourceOid;
        this.objectClassName = objectClassName;
        add(AttributeModifier.append("class", "p-0"));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSelectionModels();
        add(buildLeftPanel(getModelObject()));
        add(buildMainPanel().setOutputMarkupId(true));
    }

    //TODO
    protected void initSelectionModels() {
        selectedTuple = new LoadableModel<>() {
            @Override
            protected @NotNull ShadowAttributeTupleStatisticsType load() {
                return new ShadowAttributeTupleStatisticsType();
            }
        };

        selectedAttribute = new LoadableModel<>() {
            @Override
            protected @NotNull ShadowAttributeStatisticsType load() {
                return new ShadowAttributeStatisticsType();
            }
        };

        //temporary
        renderListViewRows(getModelObject(), true);
        renderListViewRows(getModelObject(), false);
    }

    protected List<ListViewRow> renderListViewRows(ShadowObjectClassStatisticsType statistics, boolean isAttributeTuple) {
        Stream<ListViewRow> rows = isAttributeTuple
                ? statistics.getAttributeTuple().stream().map(this::toTupleRow)
                : statistics.getAttribute().stream().map(this::toAttributeRow);

        return rows
                .sorted(Comparator.comparingInt((ListViewRow r) -> extractCount(r.subText.getObject())).reversed())
                .peek(this::initInitialSelection)
                .collect(Collectors.toList());
    }

    private @NotNull ListViewRow toTupleRow(@NotNull ShadowAttributeTupleStatisticsType item) {
        String refStr = item.getRef() != null
                ? item.getRef().stream()
                .map(r -> r.getItemPath().lastName().toString())
                .collect(Collectors.joining(" + "))
                : "";
        int combinations = item.getTupleCount() != null ? item.getTupleCount().size() : 0;
        return new ListViewRow(Model.of(refStr), Model.of(combinations + " combinations"), item);
    }

    private @NotNull ListViewRow toAttributeRow(@NotNull ShadowAttributeStatisticsType item) {
        String refStr = item.getRef().getItemPath().lastName().toString();
        int count = item.getValueCount() != null
                ? item.getValueCount().stream().mapToInt(ShadowAttributeValueCountType::getCount).sum()
                : 0;
        return new ListViewRow(Model.of(refStr), Model.of(count + " values"), item);
    }

    public record ListViewRow(IModel<String> text, IModel<String> subText, Object item) implements Serializable {
    }

    private @NotNull WebMarkupContainer buildLeftPanel(ShadowObjectClassStatisticsType statistics) {
        WebMarkupContainer left = new WebMarkupContainer(ID_LEFT_PANEL);
        left.setOutputMarkupId(true);

        left.add(buildChips(statistics));
        left.add(buildToggle());
        left.add(buildListView(statistics, selectedAttribute, selectedTuple));

        return left;
    }

    private @NotNull WebMarkupContainer buildChips(ShadowObjectClassStatisticsType statistics) {
        WebMarkupContainer chips = new WebMarkupContainer(ID_CHIPS_CONTAINER);
        chips.setOutputMarkupId(true);

        chips.add(createMetricPanel(
                ID_TOTAL_OBJECTS,
                createStringResource("SmartStatisticsPanel.totalObjects"),
                () -> String.valueOf(statistics.getSize()),
                Model.of("fa fa-cube fa-2x text-primary")));
        chips.add(createMetricPanel(
                ID_COVERAGE,
                createStringResource("SmartStatisticsPanel.coverage"),
                () -> String.format("%.2f%%", statistics.getCoverage() * 100),
                Model.of("fa fa-adjust fa-2x text-primary")));
        return chips;
    }

    private @NotNull TogglePanel<String> buildToggle() {
        IModel<List<Toggle<String>>> toggleModel = () -> List.of(
                createToggle("Tuples", isAttributeTuple),
                createToggle("Attributes", !isAttributeTuple)
        );

        return new TogglePanel<>(ID_TOGGLE_PANEL, toggleModel) {
            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<String>> item) {
                isAttributeTuple = "Tuples".equals(item.getObject().getValue());

                renderListViewRows(SmartStatisticsPanel.this.getModelObject(), isAttributeTuple);
                target.add(getListViewContainer());
                target.add(this);
                refreshMainPanel(target);
            }

            @Override
            protected String getDefaultCssClass() {
                return "btn-group d-flex w-100 border rounded";
            }

            @Override
            protected String getButtonCssClass() {
                return null;
            }

            @Override
            public @NotNull String getActiveCssClass() {
                return "btn btn-default btn-sm px-3 active";
            }

            @Override
            public String getInactiveCssClass() {
                return "btn btn-sm px-3";
            }
        };
    }

    private @NotNull Toggle<String> createToggle(String value, boolean active) {
        Toggle<String> t = new Toggle<>(null, value);
        t.setActive(active);
        t.setValue(value);
        t.setBadgeCss(active ? Badge.State.PRIMARY.getCss() : Badge.State.SECONDARY.getCss());
        return t;
    }

    private @NotNull WebMarkupContainer buildListView(
            ShadowObjectClassStatisticsType statistics,
            @NotNull IModel<ShadowAttributeStatisticsType> selectedAttribute,
            @NotNull IModel<ShadowAttributeTupleStatisticsType> selectedTuple) {
        WebMarkupContainer container = new WebMarkupContainer(ID_LIST_VIEW_CONTAINER);
        container.setOutputMarkupId(true);

        ListView<ListViewRow> listView = new ListView<>(ID_LIST_VIEW, () -> renderListViewRows(statistics, isAttributeTuple)) {
            @Override
            protected void populateItem(@NotNull ListItem<ListViewRow> item) {
                ListViewRow row = item.getModelObject();
                item.add(new Label(ID_TEXT, row.text));
                item.add(new Label(ID_SUBTEXT, row.subText));

                Object rowItem = row.item;
                if (rowItem.equals(selectedTuple.getObject()) || rowItem.equals(selectedAttribute.getObject())) {
                    item.add(AttributeModifier.append(CLASS_CSS, "cursor-pointer border-primary"));
                }

                item.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        if (row.item instanceof ShadowAttributeTupleStatisticsType tupleStats) {
                            selectedTuple.setObject(tupleStats);
                        } else if (row.item instanceof ShadowAttributeStatisticsType attrStats) {
                            selectedAttribute.setObject(attrStats);
                        }
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

    private @NotNull WebMarkupContainer buildWidgetsContainer(int totalValues, int uniqueValues, int missingValues) {
        RepeatingView widgets = new RepeatingView(ID_WIDGETS_CONTAINER);
        widgets.setOutputMarkupId(true);

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.uniqueValues"),
                () -> String.valueOf(uniqueValues),
                Model.of("")));

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.missingValues"),
                () -> String.valueOf(missingValues),
                Model.of("")));

        widgets.add(createMetricPanel(
                widgets.newChildId(),
                createStringResource("SmartStatisticsPanel.totalValues"),
                () -> String.valueOf(totalValues),
                Model.of("")));

        widgets.add(new VisibleBehaviour(() -> !isAttributeTuple));
        return widgets;
    }

    private @NotNull WebMarkupContainer buildMainPanel() {
        WebMarkupContainer main = new WebMarkupContainer(ID_MAIN_PANEL_CONTAINER);
        main.setOutputMarkupId(true);

        int total = 0, unique = 0, missing = 0;
        ShadowAttributeStatisticsType object = selectedAttribute.getObject();
        if (object != null && object.getValueCount() != null) {
            total = object.getValueCount().stream().mapToInt(ShadowAttributeValueCountType::getCount).sum();
            missing = object.getMissingValueCount();
            unique = object.getUniqueValueCount();
        }

        main.addOrReplace(buildWidgetsContainer(total, unique, missing));
        main.addOrReplace(buildPatternTable(selectedAttribute.getObject()));
        main.addOrReplace(buildFrequencyTable(selectedAttribute.getObject(), total));
        main.addOrReplace(buildTupleTable(selectedTuple.getObject()));


        return main;
    }

    private @NotNull Fragment buildPatternTable(@NotNull ShadowAttributeStatisticsType stat) {
        List<ShadowAttributeValuePatternCountType> valuePatternCount = stat.getValuePatternCount();
        List<IColumn<ShadowAttributeValuePatternCountType, String>> cols = List.of(
                badgeColumn(r -> r.getType().value()),
                new PropertyColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.value"),
                        ShadowAttributeValuePatternCountType.F_VALUE.getLocalPart(), "value"),
                new PropertyColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.count"),
                        ShadowAttributeValuePatternCountType.F_COUNT.getLocalPart(), "count")
        );
        Fragment components = tableFragment(SmartStatisticsPanel.ID_TABLE_PATTERN_CONTAINER,
                "SmartStatisticsPanel.valuePatterns.header", cols,
                new ListDataProvider<>(this, () -> valuePatternCount));
        components.add(new VisibleBehaviour(() -> !isAttributeTuple));
        return components;
    }

    private @NotNull Fragment buildFrequencyTable(@NotNull ShadowAttributeStatisticsType stat, int total) {
        List<IColumn<ShadowAttributeValueCountType, String>> cols = List.of(
                new PropertyColumn<>(createStringResource("SmartStatisticsPanel.valueFrequency.value"),
                        ShadowAttributeValueCountType.F_VALUE.getLocalPart(), "value"),
                new PropertyColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.count"),
                        ShadowAttributeValueCountType.F_COUNT.getLocalPart(), "count"),
                percentageColumn(total, ShadowAttributeValueCountType::getCount)
        );
        Fragment components = tableFragment(SmartStatisticsPanel.ID_TABLE_FREQ_CONTAINER,
                "SmartStatisticsPanel.valueFrequency.header", cols,
                new ListDataProvider<>(this, stat::getValueCount));
        components.add(new VisibleBehaviour(() -> !isAttributeTuple));
        return components;
    }

    private @NotNull Fragment buildTupleTable(@NotNull ShadowAttributeTupleStatisticsType stat) {
        List<IColumn<ShadowAttributeTupleCountType, String>> cols = new ArrayList<>();
        List<ItemPathType> refs = stat.getRef();
        for (int i = 0; i < refs.size(); i++) {
            final int index = i;
            cols.add(simpleColumn(StringUtils.capitalize(refs.get(i).getItemPath().lastName().toString()),
                    r -> safeIndex(r.getValue(), index)));
        }
        cols.add(new PropertyColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.count"),
                ShadowAttributeTupleCountType.F_COUNT.getLocalPart(), "count"));
        int total = getModelObject() != null ? getModelObject().getSize() : 0;
        cols.add(percentageColumn(total, ShadowAttributeTupleCountType::getCount));

        Fragment components = tableFragment(SmartStatisticsPanel.ID_TABLE_COMB_CONTAINER,
                "SmartStatisticsPanel.valueFrequency.header", cols,
                new ListDataProvider<>(this, stat::getTupleCount));
        components.add(new VisibleBehaviour(() -> isAttributeTuple));
        return components;
    }

    private <R> @NotNull AbstractColumn<R, String> simpleColumn(String header, SerializableFunction<R, String> getter) {
        return new AbstractColumn<>(Model.of(header)) {
            @Override
            public void populateItem(Item<ICellPopulator<R>> cell, String cid, IModel<R> row) {
                cell.add(new Label(cid, getter.apply(row.getObject())));
            }
        };
    }

    private <R> @NotNull AbstractColumn<R, String> badgeColumn(SerializableFunction<R, String> getter) {
        return new AbstractColumn<>(createStringResource("SmartStatisticsPanel.valuePatterns.type")) {
            @Override
            public void populateItem(Item<ICellPopulator<R>> cell, String cid, IModel<R> row) {
                String type = getter.apply(row.getObject());
                String css = getBadgeTypeCss(type);
                cell.add(new Label(cid, type).add(AttributeModifier.replace("class", css)));
            }
        };
    }

    protected String getBadgeTypeCss(@NotNull String type) {
        if (type.equals("prefix") || type.equals("firstToken")) {
            return "badge badge-info px-2 py-1";
        } else if (type.equals("suffix") || type.equals("lastToken")) {
            return "badge badge-success px-2 py-1";
        } else {
            return "badge badge-secondary px-2 py-1";
        }
    }

    private <R> @NotNull AbstractColumn<R, String> percentageColumn(int total, SerializableToIntFunction<R> countFn) {
        return new AbstractColumn<>(createStringResource("SmartStatisticsPanel.valueFrequency.percentage")) {
            @Override
            public void populateItem(Item<ICellPopulator<R>> cell, String cid, IModel<R> row) {
                int count = countFn.applyAsInt(row.getObject());
                String txt = total == 0 ? "0%" : String.format("%.2f%%", count * 100.0 / total);
                cell.add(new Label(cid, txt));
            }
        };
    }

    private <R extends Serializable> @NotNull Fragment tableFragment(String id, String headerKey,
            List<IColumn<R, String>> cols,
            ListDataProvider<R> provider) {
        Fragment frag = new Fragment(id, ID_TABLE_FRAGMENT, this);
        frag.add(new Label(ID_TABLE_HEADER_FRAGMENT, createStringResource(headerKey)));
        BoxedTablePanel<R> table = new BoxedTablePanel<>(ID_TABLE_PANEL_FRAGMENT, provider, cols) {
            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };
        table.setItemsPerPage(5);
        frag.add(table);
        return frag;
    }

    private @NotNull MetricValuePanel createMetricPanel(
            String id,
            IModel<String> title,
            IModel<String> valueCount,
            IModel<String> iconCss) {
        MetricValuePanel panel = new MetricValuePanel(id) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new IconWithLabel(id, title) {
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
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, valueCount);
                label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1"));
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
        AjaxLinkPanel closeButton = new AjaxLinkPanel(ID_CLOSE_BUTTON, getCloseButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        closeButton.setOutputMarkupId(true);
        closeButton.add(new VisibleBehaviour(this::isCloseButtonVisible));
        footer.add(closeButton);
        return footer;
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 60;
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
    public Component getContent() {
        return this;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("SmartStatisticsPanel.title", objectClassName.getLocalPart());
    }

    @Override
    public @Nullable Component getTitleComponent() {
        return createHeaderFragment();
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_ICON_PREVIEW);
    }

    private @NotNull Fragment createHeaderFragment() {
        StringResourceModel secondaryTitle = null;
        XMLGregorianCalendar timestamp = getModelObject().getTimestamp();
        if (timestamp != null) {
            secondaryTitle = createStringResource("SmartStatisticsPanel.title.timestamped",
                    timestamp.toXMLFormat());
        }

        Fragment header = new Fragment(SmartStatisticsPanel.ID_HEADER_FRAGMENT, ID_HEADER_FRAGMENT, this);
        header.add(new Label(ID_HEADER_PRIMARY_TITLE, getTitle()));
        header.add(new Label(ID_HEADER_SECONDARY_TITLE, secondaryTitle).setVisible(secondaryTitle != null));
        header.add(AttributeAppender.append(CLASS_CSS, "flex-grow-1"));
        return header;
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

    private String safeIndex(List<String> list, int i) {
        return list != null && list.size() > i ? list.get(i) : "";
    }

    private int extractCount(String text) {
        if (text == null) {return 0;}
        String digits = text.replaceAll("\\D+", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private void initInitialSelection(@NotNull ListViewRow row) {
        if (row.item instanceof ShadowAttributeTupleStatisticsType tupleStats) {
            if (selectedTuple.getObject().getRef() == null || selectedTuple.getObject().getRef().isEmpty()) {
                selectedTuple.setObject(tupleStats);
            }
        } else if (row.item instanceof ShadowAttributeStatisticsType attrStats) {
            if (selectedAttribute.getObject().getRef() == null) {
                selectedAttribute.setObject(attrStats);
            }
        }
    }
}
