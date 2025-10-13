/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.extensions.markup.html.tabs.ITab;

import java.util.List;

/**
 * Tabbed Panel with different html file. Split width of panel for tabs button in header.
 *
 */
public class TabCenterTabbedPanel<T extends ITab> extends AjaxTabbedPanel<T> {

    public TabCenterTabbedPanel(String id, List<T> tabs) {
        super(id, tabs);
    }
}

