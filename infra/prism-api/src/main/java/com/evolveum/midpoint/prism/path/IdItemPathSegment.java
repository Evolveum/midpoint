/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.path;

/**
 * @author semancik
 *
 */
public class IdItemPathSegment extends ItemPathSegment {

    public static final IdItemPathSegment NULL = new IdItemPathSegment();

    private Long id;

    public IdItemPathSegment() {
        this.id = null;
    }

    public IdItemPathSegment(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "[" + ( isWildcard() ? "*" : id ) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        IdItemPathSegment other = (IdItemPathSegment) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj);
    }

    public IdItemPathSegment clone() {
        IdItemPathSegment clone = new IdItemPathSegment();
        clone.id = this.id;
        return clone;
    }

}
