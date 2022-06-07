/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.behavior.AttributeAppender;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsMenuPanel extends BasePanel {

    public DetailsMenuPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "details-menu-panel d-flex rounded bg-white align-self-stretch align-self-md-start"));
    }
}
