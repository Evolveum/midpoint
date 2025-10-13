/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ColumnMenuAction<T extends Serializable> extends InlineMenuItemAction {

    private IModel<T> rowModel;

    public void setRowModel(IModel<T> rowModel) {
        this.rowModel = rowModel;
    }

    public IModel<T> getRowModel() {
        return rowModel;
    }
}
