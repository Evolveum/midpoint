/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.impl.component.data.column.icon.AbstractIconColumn;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.request.resource.IResource;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoundedIconColumn<T, S> extends AbstractIconColumn<T, S> {

    public RoundedIconColumn(IModel<String> title) {
        super(title);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String id, IModel<T> model) {
        item.add(AttributeAppender.append("style", "width: 2rem;"));

        RoundedImagePanel panel = new RoundedImagePanel(id, () -> createDisplayType(model), createPreferredImage(model)) {
            @Override
            protected String getAlternativeTextForImage() {
                return RoundedIconColumn.this.getAlternativeTextForImage(model);
            }
        };
        panel.add(AttributeAppender.append("style", "height: 2rem; width: 2rem;"));
        item.add(panel);
    }

    protected String getAlternativeTextForImage(IModel<T> model) {
        return null;
    }

    protected DisplayType createDisplayType(IModel<T> model) {
        return null;
    }

    protected IModel<IResource> createPreferredImage(IModel<T> model) {
        return () -> null;
    }

    @Override
    public String getCssClass() {
        return "rounded-icon-column";
    }
}
