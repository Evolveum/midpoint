/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ROrgClosureId implements Serializable {

    private String ancestorOid;
    private String descendantOid;

    public String getAncestorOid() {
        return ancestorOid;
    }

    public void setAncestorOid(String ancestorOid) {
        this.ancestorOid = ancestorOid;
    }

    public String getDescendantOid() {
        return descendantOid;
    }

    public void setDescendantOid(String descendantOid) {
        this.descendantOid = descendantOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROrgClosureId that = (ROrgClosureId) o;

        if (ancestorOid != null ? !ancestorOid.equals(that.ancestorOid) : that.ancestorOid != null) return false;
        if (descendantOid != null ? !descendantOid.equals(that.descendantOid) : that.descendantOid != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ancestorOid != null ? ancestorOid.hashCode() : 0;
        result = 31 * result + (descendantOid != null ? descendantOid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ROrgClosureId{" +
                "ancestorOid='" + ancestorOid + '\'' +
                ", descendantOid='" + descendantOid + '\'' +
                '}';
    }
}
