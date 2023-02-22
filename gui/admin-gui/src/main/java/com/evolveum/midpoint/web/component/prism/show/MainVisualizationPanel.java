/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MainVisualizationPanel extends SimpleVisualizationPanel {

    private static final long serialVersionUID = 1L;

    public MainVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model) {
        this(id, model, false, true);
    }

    public MainVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model, boolean showOperationalItems, boolean advanced) {
        super(id, model, showOperationalItems, advanced);
    }
}
