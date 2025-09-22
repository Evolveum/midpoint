/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResultObjects;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MetricWidgetPanel extends WidgetPanel<DashboardWidgetType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MetricWidgetPanel.class);

    private static final String ID_TITLE = "title";
    private static final String ID_MORE_INFO = "moreInfo";
    private static final String ID_TREND_BADGE = "trendBadge";
    private static final String ID_VALUE = "value";
    private static final String ID_VALUE_DESCRIPTION = "valueDescription";
    private static final String ID_ICON = "icon";
    private static final String ID_CHART_CONTAINER = "chartContainer";

    /**
     * Model with reference used as data for "more" link, if available
     */
    private IModel<ObjectReferenceType> defaultSimulationResult;

    protected IModel<List<SimulationMetricValuesType>> metricValues;

    private IModel<DisplayType> display;

    public MetricWidgetPanel(String id, IModel<DashboardWidgetType> model) {
        super(id, model);

        initModels();
        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        Component comp = get(ID_CHART_CONTAINER);
        if (comp == null || !comp.isVisibleInHierarchy()) {
            return;
        }

        Object[] array = metricValues.getObject().stream()
                .map(SimulationMetricValuesTypeUtil::getValue)
                .map(BigDecimal::doubleValue)
                .toArray();

        if (array.length == 0) {
            return;
        }

        String options = "{ height: 85, lineColor: '#92c1dc', endColor: '#92c1dc' }";
        String data = "[" + StringUtils.join(array, ", ") + "]";

        response.render(OnDomReadyHeaderItem.forScript(
                "MidPointTheme.createSparkline('#" + comp.getMarkupId() + "', " + options + ", " + data + ");"));
    }

    protected @NotNull DashboardWidgetDataType getWidgetData() {
        DashboardWidgetType widget = getModelObject();
        if (widget == null || widget.getData() == null) {
            return new DashboardWidgetDataType();
        }

        return widget.getData();
    }

    private void initModels() {
        display = new LoadableDetachableModel<>() {
            @Override
            protected DisplayType load() {
                return getDisplay();
            }
        };

        IModel<List<SimulationResultType>> results = new LoadableDetachableModel<>() {

            @Override
            protected List<SimulationResultType> load() {
                DashboardWidgetDataType data = getWidgetData();
                SimulationMetricReferenceType metricRef = data.getMetricRef();

                ObjectFilter filter = createObjectFilter(data.getCollection());
                if (filter == null || metricRef == null) {
                    return Collections.emptyList();
                }

                ObjectQuery query = getPrismContext().queryFor(SimulationResultType.class)
                        .filter(filter)
                        .asc(SimulationResultType.F_START_TIMESTAMP)
                        .maxSize(10)
                        .build();

                PageBase page = getPageBase();
                OperationResult result = page.getPageTask().getResult();
                List<PrismObject<SimulationResultType>> results = WebModelServiceUtils.searchObjects(SimulationResultType.class, query, result, page);

                return results.stream().map(po -> po.asObjectable()).collect(Collectors.toList());
            }
        };

        defaultSimulationResult = new LoadableDetachableModel<>() {
            @Override
            protected ObjectReferenceType load() {
                List<SimulationResultType> list = results.getObject();
                if (list.isEmpty()) {
                    return null;
                }

                SimulationResultType result = list.get(list.size() - 1);
                return new ObjectReferenceType()
                        .oid(result.getOid())
                        .type(SimulationResultType.COMPLEX_TYPE);
            }
        };

        metricValues = new LoadableDetachableModel<>() {

            @Override
            protected List<SimulationMetricValuesType> load() {
                DashboardWidgetDataType data = getWidgetData();
                SimulationMetricReferenceType metricRef = data.getMetricRef();

                List<SimulationResultType> simResults = results.getObject();

                return simResults.stream()
                        .map(SimulationResultType::getMetric)
                        .reduce(new ArrayList<>(), (list, metric) -> {
                            list.addAll(metric);
                            return list;
                        })
                        .stream().filter(m -> metricRef.equals(m.getRef()))
                        .collect(Collectors.toList());
            }
        };
    }

    private ObjectFilter createObjectFilter(CollectionRefSpecificationType collection) {
        if (collection == null) {
            return null;
        }

        PageBase page = getPageBase();

        SearchFilterType search;
        if (collection.getCollectionRef() != null) {
            com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType collectionRef = collection.getCollectionRef();
            PrismObject<ObjectCollectionType> obj = WebModelServiceUtils.loadObject(collectionRef, page);
            if (obj == null) {
                return null;
            }

            ObjectCollectionType objectCollection = obj.asObjectable();
            search = objectCollection.getFilter();
        } else {
            search = collection.getFilter();
        }

        if (search == null) {
            return null;
        }

        try {
            return getPageBase().getQueryConverter().createObjectFilter(SimulationResultType.class, search);
        } catch (Exception ex) {
            LOGGER.debug("Couldn't create search filter", ex);
            page.error("Couldn't create search filter, reason: " + ex.getMessage());
        }

        return null;
    }

    private DisplayType getDisplay() {
        DashboardWidgetType widget = getModelObject();
        DisplayType display = widget.getDisplay();
        if (display != null) {
            return display;
        }

        DashboardWidgetDataType data = widget.getData();
        if (data == null || data.getMetricRef() == null) {
            return new DisplayType().label("MetricWidgetPanel.unnamed");
        }

        SimulationMetricReferenceType metricRef = data.getMetricRef();
        ObjectReferenceType eventMarkRef = metricRef.getEventMarkRef();
        String metricIdentifier = metricRef.getIdentifier();
        if (eventMarkRef != null) {
            PrismObject<MarkType> markObject =
                    WebModelServiceUtils.loadObject(eventMarkRef, getPageBase());
            if (markObject != null) {
                MarkType mark = markObject.asObjectable();
                DisplayType d = mark.getDisplay();
                return d != null ? d : new DisplayType().label(mark.getName());
            } else {
                return new DisplayType().label(new PolyStringType(WebComponentUtil.getName(eventMarkRef)));
            }
        } else if (metricIdentifier != null) {
            SimulationMetricDefinitionType def =
                    getPageBase().getSimulationResultManager().getMetricDefinition(metricIdentifier);

            if (def != null) {
                DisplayType d = def.getDisplay();
                return d != null ? d : new DisplayType().label(new PolyStringType(def.getIdentifier()));
            } else {
                return new DisplayType().label(new PolyStringType(metricIdentifier));
            }
        } else if (metricRef.getBuiltIn() != null) {
            return new DisplayType().label(new PolyStringType(LocalizationUtil.createKeyForEnum(metricRef.getBuiltIn())));
        }

        return null;
    }

    private void initLayout() {
        add(AttributeModifier.prepend("class", "metric-widget d-flex flex-column border rounded"));

        IModel<String> titleModel = () -> {
            DisplayType d = display.getObject();
            return d != null ? LocalizationUtil.translatePolyString(d.getLabel()) : null;
        };

        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.append("title", titleModel));
        add(title);

        // todo implement properly and make visible
        BadgePanel trendBadge = new BadgePanel(ID_TREND_BADGE, () -> {
            Badge badge = new Badge();
            badge.setCssClass("badge badge-success trend trend-success");
            badge.setIconCssClass("fa-solid fa-arrow-trend-up mr-1");
            badge.setText("+3,14%");
            return badge;
        });
        trendBadge.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(trendBadge);

        IModel<String> valueModel = createValueModel();
        Label value = new Label(ID_VALUE, () -> {
            String v = valueModel.getObject();
            return v != null ? v : LocalizationUtil.translate("MetricWidgetPanel.noValue");
        });
        value.add(AttributeAppender.append("class", () -> hasZeroValue(valueModel) ? "text-secondary" : "text-bold"));
        add(value);

        IModel<String> descriptionModel = () -> {
            DisplayType d = display.getObject();
            return d != null ? LocalizationUtil.translatePolyString(d.getTooltip()) : null;
        };
        Label valueDescription = new Label(ID_VALUE_DESCRIPTION, descriptionModel);
        add(valueDescription);

        IModel<CompositedIcon> iconModel = this::createIcon;

        CompositedIconPanel icon = new CompositedIconPanel(ID_ICON, iconModel);
        icon.add(new VisibleBehaviour(() -> isIconVisible(iconModel)));
        add(icon);

        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CHART_CONTAINER);
        chartContainer.add(new VisibleBehaviour(() -> !isIconVisible(iconModel)));
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);

        AjaxLink<Void> moreInfo = new AjaxLink<>(ID_MORE_INFO) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoreInfoPerformed(target);
            }
        };
        moreInfo.add(new EnableBehaviour(() -> isMoreInfoVisible()));
        moreInfo.add(AttributeAppender.append("class", () -> hasZeroValue(valueModel) ? "invisible" : "text-primary"));
        add(moreInfo);
    }

    private boolean hasZeroValue(IModel<String> valueModel) {
        String value = valueModel.getObject();
        return StringUtils.isEmpty(value) || "0".equals(value);
    }

    private boolean isIconVisible(IModel<CompositedIcon> iconModel) {
        if (iconModel.getObject() == null) {
            return false;
        }

        return metricValues.getObject().size() <= 1;
    }

    private IModel<String> createValueModel() {
        return () -> {
            DashboardWidgetDataType data = getModelObject().getData();
            if (data != null && data.getStoredData() != null) {
                return data.getStoredData();
            }

            List<SimulationMetricValuesType> values = metricValues.getObject();
            if (values.isEmpty()) {
                return null;
            }

            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(values.get(values.size() - 1));

            return formatValue(value, LocalizationUtil.findLocale());
        };
    }

    public static String formatValue(Number value, Locale locale) {
        NumberFormat numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setMaximumFractionDigits(3);
        return numberFormat.format(value);
    }

    protected boolean isMoreInfoVisible() {
        return defaultSimulationResult.getObject() != null;
    }

    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
        ObjectReferenceType ref = defaultSimulationResult.getObject();
        if (ref == null) {
            return;
        }

        DashboardWidgetDataType data = getWidgetData();
        SimulationMetricReferenceType metricRef = data.getMetricRef();
        ObjectReferenceType markRef = metricRef != null ? metricRef.getEventMarkRef() : null;

        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, ref.getOid());

        ObjectProcessingStateType state = SimulationsGuiUtil.builtInMetricToProcessingState(metricRef.getBuiltIn());
        if (state != null) {
            params.set(PageSimulationResultObjects.PAGE_QUERY_PARAMETER, state.value());
            getPageBase().navigateToNext(PageSimulationResultObjects.class, params);
            return;
        }

        if (markRef != null) {
            params.add(SimulationPage.PAGE_PARAMETER_MARK_OID, markRef.getOid());
            getPageBase().navigateToNext(PageSimulationResultObjects.class, params);
            return;
        }

        getPageBase().navigateToNext(PageSimulationResult.class, params);
    }

    private CompositedIcon createIcon() {
        DisplayType d = display.getObject();
        if (d == null) {
            return null;
        }

        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(d), IconCssStyle.CENTER_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(d));

        return builder.build();
    }
}
