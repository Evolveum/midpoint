/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.border.Border;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationsPanel extends Border {

    public OperationsPanel(String id) {
        super(id);

        add(AttributeAppender.prepend("class", "card"));
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.setName("div");
        super.onComponentTag(tag);
    }
}
