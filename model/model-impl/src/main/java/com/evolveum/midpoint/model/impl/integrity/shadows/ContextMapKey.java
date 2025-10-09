/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;

class ContextMapKey implements Serializable {

    final String resourceOid;
    final QName objectClassName;

    ContextMapKey(String resourceOid, QName objectClassName) {
        this.resourceOid = resourceOid;
        this.objectClassName = objectClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ContextMapKey))
            return false;
        ContextMapKey that = (ContextMapKey) o;
        return Objects.equals(resourceOid, that.resourceOid) &&
                Objects.equals(objectClassName, that.objectClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceOid, objectClassName);
    }

    @Override
    public String toString() {
        return "ContextMapKey{" +
                "resourceOid='" + resourceOid + '\'' +
                ", objectClassName=" + objectClassName +
                '}';
    }
}
