/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.extensions.markup.html.tabs.ITab;

import java.util.List;

/**
 * A variant of {@link TabbedPanel} that renders tabs in a standalone card layout,
 * separating the tab header panel from the main card content and omitting default CSS classes.
 */
public class TabSeparatedTabbedPanel<T extends ITab> extends AjaxTabbedPanel<T> {

    public TabSeparatedTabbedPanel(String id, List<T> tabs) {
        super(id, tabs);
    }

    @Override
    protected void initDefaultComponentCssClass() {
        // do nothing
    }
}

