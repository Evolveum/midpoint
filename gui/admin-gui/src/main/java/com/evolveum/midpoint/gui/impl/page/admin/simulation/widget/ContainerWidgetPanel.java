/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerWidgetDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WidgetType;

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
        add(AttributeModifier.prepend("class", "d-flex gap-3"));

        add(AttributeModifier.append("class", () -> {
            ContainerWidgetDirectionType direction = getModelObject().getDirection();

            return direction == ContainerWidgetDirectionType.COLUMN ? "flex-column" : "flex-row";
        }));

        Label title = new Label(ID_TITLE);  // todo model
        add(title);

        ListView<? extends WidgetType> widgets = new ListView<>(ID_WIDGETS) { // todo model

            @Override
            protected void populateItem(ListItem<WidgetType> item) {
                WidgetPanel<? extends WidgetType> widget = createWidgetPanel(ID_WIDGET, item.getModel());

                if (widget != null) {
                    item.add(widget);
                }
            }
        };
        add(widgets);
    }

    private WidgetPanel<? extends WidgetType> createWidgetPanel(String id, IModel<WidgetType> model) {
        WidgetType w = model.getObject();

        // todo fix via some widget factory...
        if (w instanceof ContainerWidgetType) {
            return new ContainerWidgetPanel(id, () -> (ContainerWidgetType) model.getObject());
        } else if (w instanceof SimulationMetricWidgetType) {
            return new SimulationMetricWidgetPanel(id, () -> (SimulationMetricWidgetType) model.getObject());
        } else {
            return null;
        }
    }
}
