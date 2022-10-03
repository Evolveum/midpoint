/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailableFilterType;

public class AvailableFilterWrapper implements SelectableRow<AvailableFilterType>{

    private boolean selected;
    private AvailableFilterType filter;

    public AvailableFilterWrapper(AvailableFilterType filter) {
        this.filter = filter;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public AvailableFilterType getFilter() {
        return filter;
    }
}
