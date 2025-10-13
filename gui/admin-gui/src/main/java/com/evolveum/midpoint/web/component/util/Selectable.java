/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

/**
 * @author lazyman
 */
public abstract class Selectable<S> implements Serializable {

   private static final long serialVersionUID = 1L;

    public static final String F_SELECTED = "selected";

    private boolean selected;
    private OperationResult result;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public S getValue() {
        return (S) this;
    }

}
