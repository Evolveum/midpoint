/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents various forms of association name: local, qualified, standard path.
 *
 * We assume that the name can be used for both the dummy resource (like `contract`)
 * and the respective schema (like `ri:contract`).
 */
@Experimental
public class AssocName {

    @NotNull private final ItemName itemName;

    private AssocName(@NotNull ItemName itemName) {
        this.itemName = itemName;
    }

    public static AssocName ri(String localPart) {
        return new AssocName(new ItemName(SchemaConstants.NS_RI, localPart));
    }

    public String local() {
        return itemName.getLocalPart();
    }

    public ItemName q() {
        return itemName;
    }

    public ItemPath associationPath() {
        return ItemPath.create(ShadowType.F_ASSOCIATIONS, q());
    }
}
