/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
class ItemPathSegmentUtil {
    public static boolean isName(Object segment) {
        return segment instanceof NameItemPathSegment || segment instanceof QName
                && !isSpecialName(segment); // todo remove
    }

    @Contract("_, true -> !null")
    static ItemName toName(Object segment, boolean failOnError) {
        if (segment instanceof NameItemPathSegment) {
            return ((NameItemPathSegment) segment).getName();
        } else if (segment instanceof ItemName) {
            return (ItemName) segment;
        } else if (segment instanceof QName) {
            return ItemName.fromQName((QName) segment);
        } else {
            if (failOnError) {
                throw new IllegalArgumentException("Not a name: " + getStringInformation(segment));
            } else {
                return null;
            }
        }
    }

    static boolean isSpecial(Object o) {
        return o instanceof IdentifierPathSegment || o instanceof ReferencePathSegment
                || isSpecialName(o);    // todo remove
    }

    public static boolean isSpecialName(Object o) {
        return IdentifierPathSegment.QNAME.equals(o) || ObjectReferencePathSegment.QNAME.equals(o) || ParentPathSegment.QNAME.equals(o);
    }

    static boolean isParent(Object o) {
        return o instanceof ParentPathSegment || ParentPathSegment.QNAME.equals(o);
    }

    public static boolean isObjectReference(Object o) {
        return o instanceof ObjectReferencePathSegment || ObjectReferencePathSegment.QNAME.equals(o);
    }

    public static boolean isIdentifier(Object o) {
        return o instanceof IdentifierPathSegment || IdentifierPathSegment.QNAME.equals(o);
    }

    public static boolean isId(Object o) {
        return o == null || o instanceof IdItemPathSegment || o instanceof Long || o instanceof Integer;
    }

    public static boolean isNullId(Object o) {
        return o == null || o instanceof IdItemPathSegment && ((IdItemPathSegment) o).getId() == null;
    }

    public static Long toId(Object o, boolean failOnError) {
        if (o instanceof IdItemPathSegment) {
            return ((IdItemPathSegment) o).getId();
        } else if (o == null || o instanceof Long) {
            return (Long) o;
        } else if (o instanceof Integer) {
            return ((Integer) o).longValue();
        } else {
            if (failOnError) {
                throw new IllegalArgumentException("Not an ID: " + o);
            } else {
                return null;
            }
        }
    }

    public static boolean isVariable(Object o) {
        return o instanceof VariableItemPathSegment;
    }

    public static QName toVariableName(Object segment) {
        if (segment instanceof VariableItemPathSegment) {
            return ((VariableItemPathSegment) segment).getName();
        } else {
            throw new IllegalArgumentException("Not a variable: " + getStringInformation(segment));
        }
    }

    public static QName getSpecialSymbol(Object o) {
        if (o instanceof QName) {
            return (QName) o;
        } else if (o instanceof IdentifierPathSegment) {
            return IdentifierPathSegment.QNAME;
        } else if (o instanceof ParentPathSegment) {
            return ParentPathSegment.QNAME;
        } else if (o instanceof ObjectReferencePathSegment) {
            return ObjectReferencePathSegment.QNAME;
        } else {
            throw new IllegalArgumentException("Not a special item path segment: " + getStringInformation(o));
        }
    }

    @NotNull
    public static String getStringInformation(Object o) {
        return o + (o != null ? " (" + o.getClass() + ")" : "");
    }
}
