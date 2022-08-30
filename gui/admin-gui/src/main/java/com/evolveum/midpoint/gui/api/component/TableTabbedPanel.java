/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Optional;

/**
 * @author lskublik
 */
public class TableTabbedPanel<T extends ITab> extends AjaxTabbedPanel<T> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";

    public TableTabbedPanel(final String id, final List<T> tabs) {
        super(id, tabs);
    }

    @Override
    protected void populateLoopItem(LoopItem item) {
        super.populateLoopItem(item);
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        ((WebMarkupContainer)item.get(ID_LINK)).add(icon);

        final int index = item.getIndex();

        String iconClass = getIcon(index);
        icon.add(AttributeAppender.append("class", iconClass));
        icon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(iconClass)));
    }

    private String getIcon(int index) {
        return null;
    }
}

