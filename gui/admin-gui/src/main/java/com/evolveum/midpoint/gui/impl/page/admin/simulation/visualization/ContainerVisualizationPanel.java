/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ContainerVisualizationPanel extends CardOutlineLeftPanel<ContainerVisualization> {

    public ContainerVisualizationPanel(String id, IModel<ContainerVisualization> model) {
        super(id, model);
    }
}
