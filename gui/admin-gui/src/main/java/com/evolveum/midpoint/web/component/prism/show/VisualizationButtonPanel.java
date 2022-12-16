/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

/**
 * @author mserbak
 * @author lazyman
 */
public class VisualizationButtonPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public static final String ID_MINIMIZE_BUTTON = "minimizeButton";
    public static final String ID_ICON = "icon";

    public VisualizationButtonPanel(String id, IModel<VisualizationDto> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<VisualizationDto> model) {
        AjaxLink<String> minimize = new AjaxLink<String>(ID_MINIMIZE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                minimizeOnClick(target);
            }
        };
        add(minimize);

        Label icon = new Label(ID_ICON);
        icon.add(AttributeModifier.append("class", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                VisualizationDto dto = model.getObject();
                if (dto.isMinimized()) {
                    return GuiStyleConstants.CLASS_ICON_EXPAND;
                }

                return GuiStyleConstants.CLASS_ICON_COLLAPSE;
            }
        }));
        minimize.add(icon);

        icon.add(new AttributeAppender("title", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                VisualizationDto dto = model.getObject();
                if (dto.isMinimized()) {
                    return getString("prismOptionButtonPanel.maximize");
                }
                return getString("prismOptionButtonPanel.minimize");
            }
        }, ""));
    }

    public void minimizeOnClick(AjaxRequestTarget target) {
    }
}
