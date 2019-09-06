/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;

/**
 * @author lazyman
 */
public abstract class Selectable<S> implements Serializable {

   private static final long serialVersionUID = 1L;

	public static final String F_SELECTED = "selected";

    private boolean selected;

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
