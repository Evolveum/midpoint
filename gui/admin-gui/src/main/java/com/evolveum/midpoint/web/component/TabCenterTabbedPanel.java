/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Optional;

/**
 * Tabbed Panel with different html file. Split width of panel for tabs button in header.
 *
 */
public class TabCenterTabbedPanel<T extends ITab> extends AjaxTabbedPanel<T> {

    public TabCenterTabbedPanel(String id, List<T> tabs) {
        super(id, tabs);
    }
}

