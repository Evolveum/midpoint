/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.objects;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO better name?
 */
public class ObjectInfo {

    private final String oid;
    private final String name;

    ObjectInfo(ObjectType object) {
        oid = object.getOid();
        name = PolyString.getOrig(object.getName());
    }

    public String getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + "(" + oid + ")";
    }
}
