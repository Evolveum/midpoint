/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.path;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class NameItemPathSegment extends ItemPathSegment {

    @NotNull private final ItemName name;

    public NameItemPathSegment(@NotNull QName name) {
        this.name = ItemName.fromQName(name);
    }

    @NotNull
    public ItemName getName() {
        return name;
    }

    @Override
    public String toString() {
        return DebugUtil.formatElementName(name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        // if we need to compute hash from namespace-normalized name, we would use this one:
        // (in order for equals to work; if we decide to change equals in such a way later)
        // result = prime * result + ((name == null) ? 0 : name.getLocalPart().hashCode());

        // this version is for "precise" equals
        result = prime * result + name.hashCode();
        return result;
    }

    /**
     * More strict version of comparison: it requires exact matching of QNames (e.g. x:xyz and xyz are different in this respect).
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return equals(obj, false, false);
    }

    /**
     * Less strict version of comparison: it allows unqualified names to match fully qualified ones (e.g. x:xyz and xyz are the same).
     *
     * @param obj
     * @return
     */

    @Override
    public boolean equivalent(Object obj) {
        return equals(obj, true, true);
    }

    public boolean equals(Object obj, boolean allowUnqualified, boolean allowDifferentPrefixes) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NameItemPathSegment other = (NameItemPathSegment) obj;
        if (allowUnqualified) {
            if (!allowDifferentPrefixes) {
                throw new UnsupportedOperationException("It is not possible to disallow different prefixes while allowing unqualified names");
            }
            return QNameUtil.match(name, other.name);
        } else {
            if (!name.equals(other.name)) {         // compares namespace and local part
                return false;
            }
            // in order to differentiate between x:name and name (when x is undefined) we will compare the prefixes as well
            if (!allowDifferentPrefixes && !normalizedPrefix(name).equals(normalizedPrefix(other.name))) {
                return false;
            }
            return true;
        }
    }

    private String normalizedPrefix(QName name) {
        if (name.getPrefix() == null) {
            return "";
        } else {
            return name.getPrefix();
        }
    }

    public NameItemPathSegment clone() {
        NameItemPathSegment clone = new NameItemPathSegment(this.name);
        clone.setWildcard(this.isWildcard());
        return clone;
    }

}
