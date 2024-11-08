/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.impl.component.data.column.icon.AbstractIconColumn;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class LinkIconColumn<T extends Serializable> extends AbstractIconColumn<T, String> {

    public LinkIconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, final IModel<T> rowModel) {
        cellItem.add(new LinkIconPanel(componentId, createIconModel(rowModel), createTitleModel(rowModel)) {

            @Override
            protected void onClickPerformed(AjaxRequestTarget target) {
                LinkIconColumn.this.onClickPerformed(target, rowModel, getLink());
            }
        });
    }

    protected IModel<String> createTitleModel(final IModel<T> rowModel) {
        return null;
    }

    protected IModel<String> createIconModel(final IModel<T> rowModel) {
        throw new UnsupportedOperationException("Not implemented, please implement in your column.");
    }

    protected void onClickPerformed(AjaxRequestTarget target, IModel<T> rowModel, AjaxLink link) {

    }
}
