/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
