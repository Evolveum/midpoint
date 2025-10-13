/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

public class AdHocDataItem extends DataItem {

    @NotNull private final ItemPath itemPath;

    public AdHocDataItem(@NotNull ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    @NotNull
    public ItemPath getItemPath() {
        return itemPath;
    }
}
