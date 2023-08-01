/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.dto;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.Serializable;

public class VerificationAttributeDto implements Serializable {

    public static final String F_VALUE = "value";
    private final ItemPath itemPath;
    private String value;

    public VerificationAttributeDto(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public String getValue() {
        return value;
    }
}
