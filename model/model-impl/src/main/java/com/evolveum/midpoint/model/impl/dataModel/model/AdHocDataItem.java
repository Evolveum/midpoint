/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
