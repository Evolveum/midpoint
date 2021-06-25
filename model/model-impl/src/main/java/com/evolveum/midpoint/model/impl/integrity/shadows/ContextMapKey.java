/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
