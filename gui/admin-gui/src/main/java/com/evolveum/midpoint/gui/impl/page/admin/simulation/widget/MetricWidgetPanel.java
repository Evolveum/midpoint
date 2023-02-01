/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MetricWidgetPanel extends WidgetPanel<DashboardWidgetType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_OPEN = "open";
    private static final String ID_TREND_BADGE = "trendBadge";
    private static final String ID_VALUE = "value";
    private static final String ID_VALUE_DESCRIPTION = "valueDescription";
    private static final String ID_ICON = "";
    private static final String ID_CHART_CONTAINER = "chartContainer";

    public MetricWidgetPanel(String id, IModel<DashboardWidgetType> model) {
        super(id, model);

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

        Object[] array = new Object[0]; // todo data

        if (array == null || array.length == 0) {
            return;
        }

        String options = "{ height: 85, lineColor: '#92c1dc', endColor: '#92c1dc' }";
        String data = "[" + StringUtils.join(array, ", ") + "]";

        response.render(OnDomReadyHeaderItem.forScript(
                "MidPointTheme.createSparkline('#" + comp.getMarkupId() + "', " + options + ", " + data + ");"));
    }

    private void initLayout() {
        add(AttributeModifier.prepend("class", "d-flex flex-column border rounded bg-white"));

        Label title = new Label(ID_TITLE, () -> {
            DisplayType display = getModelObject().getDisplay();
            return display != null ? WebComponentUtil.getTranslatedPolyString(display.getLabel()) : "Some thing or other";  // todo fix
        });
        add(title);

        AjaxLink open = new AjaxLink<>(ID_OPEN) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onOpenPerformed(target);
            }
        };
        open.add(new VisibleBehaviour(() -> true)); // todo visible only when it's event mark ref metric
        add(open);

        BadgePanel trendBadge = new BadgePanel(ID_TREND_BADGE, () -> {
            Badge badge = new Badge();
            badge.setCssClass("badge badge-success trend trend-success");
            badge.setIconCssClass("fa-solid fa-arrow-trend-up mr-1");
            badge.setText("+3,14%");
            return badge;
        });
        add(trendBadge);

        Label value = new Label(ID_VALUE, createValueModel());
        add(value);

        Label valueDescription = new Label(ID_VALUE_DESCRIPTION, createDescriptionModel());
        add(valueDescription);

        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CHART_CONTAINER);
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);
    }

    private IModel<String> createValueModel() {
        return () -> "asdf";
    }

    private IModel<String> createDescriptionModel() {
        return () -> "jklo";
    }

    protected void onOpenPerformed(AjaxRequestTarget target) {

    }
}
