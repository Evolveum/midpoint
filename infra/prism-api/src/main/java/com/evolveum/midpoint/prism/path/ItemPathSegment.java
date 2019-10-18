/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.path;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public abstract class ItemPathSegment implements Serializable, Cloneable {

    private boolean wildcard = false;

    public boolean isWildcard() {
        return wildcard;
    }

    protected void setWildcard(boolean wildcard) {
        this.wildcard = wildcard;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (wildcard ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ItemPathSegment other = (ItemPathSegment) obj;
        if (wildcard != other.wildcard)
            return false;
        return true;
    }

    public abstract boolean equivalent(Object obj);

    public abstract ItemPathSegment clone();
}
