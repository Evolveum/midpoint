/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
