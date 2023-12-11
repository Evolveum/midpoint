/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cleanup;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;

public class CleanupEvent {

    private final Item<?, ?> item;

    private final ItemPath path;

    public CleanupEvent(Item<?, ?> item, ItemPath path) {
        this.item = item;
        this.path = path;
    }

    public Item<?, ?> getItem() {
        return item;
    }

    public ItemPath getPath() {
        return path;
    }
}
