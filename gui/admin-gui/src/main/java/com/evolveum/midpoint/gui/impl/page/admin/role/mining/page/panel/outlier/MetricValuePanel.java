/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class MetricValuePanel extends BasePanel<String> {

    private static final String ID_TITLE = "title";
    private static final String ID_VALUE = "value";

    public MetricValuePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Component title = getTitleComponent(ID_TITLE);
        title.setOutputMarkupId(true);
        add(title);

        Component value = getValueComponent(ID_VALUE);
        value.setOutputMarkupId(true);
        add(value);
    }

    protected Component getTitleComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getValueComponent(String id) {
        return new WebMarkupContainer(id);
    }

}
