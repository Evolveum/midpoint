/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.model;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public interface SelectableRowModel<T> extends IModel<T> {

    boolean isSelected();
    void setSelected(boolean selected);
}
