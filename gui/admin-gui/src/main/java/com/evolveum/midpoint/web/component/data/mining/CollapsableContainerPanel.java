/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.mining;

import java.io.Serial;

import org.apache.wicket.markup.html.WebMarkupContainer;

public class CollapsableContainerPanel extends WebMarkupContainer {
    boolean isExpanded = false;

    @Serial private static final long serialVersionUID = 1L;

    public CollapsableContainerPanel(String id) {
        super(id);
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

}
