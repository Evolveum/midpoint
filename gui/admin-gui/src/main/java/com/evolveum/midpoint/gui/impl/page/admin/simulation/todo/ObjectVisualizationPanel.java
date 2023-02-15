/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectVisualizationPanel extends CardOutlineLeftPanel<ObjectVisualization> {

    private boolean headless;

    public ObjectVisualizationPanel(String id, IModel<ObjectVisualization> model) {
        this(id, model, false);
    }

    public ObjectVisualizationPanel(String id, IModel<ObjectVisualization> model, boolean headless) {
        super(id, model);

        this.headless = headless;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card-outline-left-success"));
    }
}
