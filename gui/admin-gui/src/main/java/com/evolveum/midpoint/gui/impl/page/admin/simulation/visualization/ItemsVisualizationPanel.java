/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ItemsVisualizationPanel extends CardOutlineLeftPanel<VisualizationDto> {

    public ItemsVisualizationPanel(String id, IModel<VisualizationDto> model) {
        super(id, model);
    }

    @Override
    protected @NotNull IModel<String> createIconModel() {
        return () -> "fa-solid fa-user mr-1";
    }

    @Override
    protected @NotNull IModel<String> createTitleModel() {
        return () -> getString("ItemsVisualizationPanel.title");
    }

    @Override
    protected @NotNull IModel<String> createCardOutlineCssModel() {
        return () -> VisualizationGuiUtil.createChangeTypeCssClassForOutlineCard(getModelObject().getChangeType());
    }
}
