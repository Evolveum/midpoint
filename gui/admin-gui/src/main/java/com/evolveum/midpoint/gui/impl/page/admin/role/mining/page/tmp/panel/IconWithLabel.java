/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import java.io.Serial;

public class IconWithLabel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "label";
    private static final String ID_ICON = "icon";

    public IconWithLabel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex align-items-center gap-1"));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace("class", getIconCssClass()));
        image.setOutputMarkupId(true);
        add(image);

        Label label = new Label(ID_TEXT, getModel());
        label.setOutputMarkupId(true);
        add(label);
    }

    public String getIconCssClass() {
        return "";
    }
}
