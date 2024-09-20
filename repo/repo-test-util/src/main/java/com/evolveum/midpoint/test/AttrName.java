/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Represents various forms of attribute name: local, qualified, standard path.
 *
 * We assume that the name can be used for both the dummy resource (like loot) and the respective schema (like ri:loot).
 */
@Experimental
public class AttrName {

    @NotNull private final ItemName itemName;

    private AttrName(@NotNull ItemName itemName) {
        this.itemName = itemName;
    }

    public static AttrName ri(String localPart) {
        return new AttrName(new ItemName(SchemaConstants.NS_RI, localPart));
    }

    public static AttrName icfsName() {
        return icfs("name");
    }

    public static AttrName icfs(String localPart) {
        return new AttrName(new ItemName(SchemaConstants.NS_ICFS, localPart));
    }

    public String local() {
        return itemName.getLocalPart();
    }

    public ItemName q() {
        return itemName;
    }

    public ItemPath path() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, q());
    }
}
