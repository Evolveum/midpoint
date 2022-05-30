/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogHeaderPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_REQUESTING_FOR = "requestingFor";
    private static final String ID_SEARCH = "search";

    public RoleCatalogHeaderPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex flex-wrap gap-3 justify-content-between align-items-center"));

        add(new Label(ID_REQUESTING_FOR, () -> "Requesting for 4 users"));

        add(new Label(ID_SEARCH, () -> "todo search panel"));
    }
}
