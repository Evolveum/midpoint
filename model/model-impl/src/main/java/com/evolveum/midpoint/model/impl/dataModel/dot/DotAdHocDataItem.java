/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.AdHocDataItem;
import org.jetbrains.annotations.NotNull;

public class DotAdHocDataItem implements DotDataItem {

    @NotNull private final AdHocDataItem dataItem;

    public DotAdHocDataItem(@NotNull AdHocDataItem dataItem) {
        this.dataItem = dataItem;
    }

    @Override
    public String getNodeName() {
        return "\"Unresolved: " + dataItem.getItemPath() + "\"";
    }

    @Override
    public String getNodeLabel() {
        return String.valueOf(dataItem.getItemPath());
    }

    @Override
    public String getNodeStyleAttributes() {
        return "";
    }

}
