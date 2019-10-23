/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;

import com.evolveum.midpoint.web.component.util.Choiceable;

/**
 * @author lazyman
 */
public class ResourceItemDto implements Serializable, Choiceable, Comparable<ResourceItemDto> {

    private String oid;
    private String name;

    public ResourceItemDto(String oid, String name) {
        this.name = name;
        this.oid = oid;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    @Override
    public int compareTo(ResourceItemDto o) {
        return String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
    }
}
