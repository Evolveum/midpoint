/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsMenuItemPanel extends BasePanel {

    public DetailsMenuItemPanel(String id) {
        super(id);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "li");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "border-bottom"));
    }
}
