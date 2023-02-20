/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;

/**
 * @author lazyman
 */
public class VisualizationButtonPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public static final String ID_MINIMIZE = "minimize";

    public VisualizationButtonPanel(String id, IModel<VisualizationDto> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<VisualizationDto> model) {
        AjaxIconButton minimize = new AjaxIconButton(ID_MINIMIZE,
                () -> model.getObject().isMinimized() ? GuiStyleConstants.CLASS_ICON_EXPAND : GuiStyleConstants.CLASS_ICON_COLLAPSE,
                () -> model.getObject().isMinimized() ? getString("prismOptionButtonPanel.maximize") : getString("prismOptionButtonPanel.minimize")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                minimizeOnClick(target);
            }
        };
        add(minimize);
    }

    public void minimizeOnClick(AjaxRequestTarget target) {
    }
}
