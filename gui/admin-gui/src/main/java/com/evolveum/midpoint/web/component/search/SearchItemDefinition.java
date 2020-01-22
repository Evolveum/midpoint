/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public class SearchItemDefinition implements Serializable {

    private ItemPath path;
    private ItemDefinition def;

    private List<?> allowedValues;

    public SearchItemDefinition(ItemPath path, ItemDefinition def, List<?> allowedValues) {
        this.path = path;
        this.def = def;
        this.allowedValues = allowedValues;
    }

    public ItemPath getPath() {
        return path;
    }

    public ItemDefinition getDef() {
        return def;
    }

    public List<?> getAllowedValues() {
        return allowedValues;
    }


}
