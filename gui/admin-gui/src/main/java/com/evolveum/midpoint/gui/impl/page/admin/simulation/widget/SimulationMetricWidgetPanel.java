/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricWidgetType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationMetricWidgetPanel extends WidgetPanel<SimulationMetricWidgetType> {

    private static final long serialVersionUID = 1L;

    private static final String ID = "";

    public SimulationMetricWidgetPanel(String id, IModel<SimulationMetricWidgetType> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {

    }
}
