/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ContainerWidgetPanel extends WidgetPanel<ContainerWidgetType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_WIDGETS = "widgets";
    private static final String ID_WIDGET = "widget";

    public ContainerWidgetPanel(@NotNull String id, @NotNull IModel<ContainerWidgetType> model) {
        super(id, () -> {
            ContainerWidgetType cw = model.getObject();
            return cw != null ? cw : new ContainerWidgetType();
        });
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeModifier.prepend("class", () -> {
            ContainerWidgetDirectionType direction = getModelObject().getDirection();

            String directionCss = direction == ContainerWidgetDirectionType.COLUMN ? "flex-column" : "flex-row";

            return "d-flex gap-3 " + directionCss;
        }));

        Label title = new Label(ID_TITLE);  // todo model
        add(title);

        ListView<WidgetType> widgets = new ListView<>(ID_WIDGETS, () -> {
            WidgetsType ws = getModelObject().getWidgets();
            return ws != null ?
                    ws.getWidget().stream().map(je -> je.getValue()).collect(Collectors.toUnmodifiableList())
                    : Collections.emptyList();
        }) { // todo model

            @Override
            protected void populateItem(ListItem<WidgetType> item) {
                Component widget = createWidgetPanel(ID_WIDGET, item.getModel());

                if (widget != null) {
                    item.add(widget);
                }
            }
        };
        add(widgets);
    }

    public static Component createWidgetPanel(String id, IModel<WidgetType> model) {
        WidgetType w = model.getObject();

        // todo fix via some widget factory...
//        if (w instanceof ContainerWidgetType) {
//            return new ContainerWidgetPanel(id, () -> (ContainerWidgetType) model.getObject());
//        } else if (w instanceof SimulationMetricWidgetType) {
//            return new MetricWidgetPanel(id, () -> (SimulationMetricWidgetType) model.getObject());
//        } else {
            return new Label(id, () -> model.getObject().getIdentifier());
//        }
    }
}
