/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationPanelPart extends Border {

    private static final String ID_LEGEND = "legend";

    public OperationPanelPart(String id, IModel<String> legend) {
        super(id);

        add(AttributeAppender.prepend("class", "objectButtons d-flex gap-1 align-items-start"));
        addToBorder(new Label(ID_LEGEND, () -> legend != null ? legend.getObject() : null));
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.setName("fieldset");
        super.onComponentTag(tag);
    }
}
