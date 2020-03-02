/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class ItemName extends QName implements ItemPath {

    public ItemName(String namespaceURI, String localPart) {
        super(namespaceURI, localPart);
    }

    public ItemName(String namespaceURI, String localPart, String prefix) {
        super(namespaceURI, localPart, prefix);
    }

    public ItemName(String localPart) {
        super(localPart);
    }

    public ItemName(@NotNull QName name) {
        this(name.getNamespaceURI(), name.getLocalPart(), name.getPrefix());
    }

    public static ItemName fromQName(QName name) {
        if (name == null) {
            return null;
        } else if (name instanceof ItemName) {
            return (ItemName) name;
        } else {
            return new ItemName(name);
        }
    }

//    public static ItemName fromString(String name) {
//        if (name == null) {
//            return null;
//        } else {
//            return new ItemName(name);
//        }
//    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @NotNull
    @Override
    public List<?> getSegments() {
        return Collections.singletonList(new QName(getNamespaceURI(), getLocalPart(), getPrefix()));      // todo eliminate QName construction while avoiding endless recursion
    }

    @Override
    public Object getSegment(int i) {
        if (i == 0) {
            return this;
        } else {
            throw new IndexOutOfBoundsException("Index: " + i + ", while accessing single-item path");
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Object first() {
        return this;
    }

    @NotNull
    @Override
    public ItemPath rest() {
        return ItemPath.EMPTY_PATH;
    }

    @NotNull
    @Override
    public ItemPath rest(int n) {
        if (n == 0) {
            return this;
        } else {
            return EMPTY_PATH;
        }
    }

    @Override
    public Long firstToIdOrNull() {
        return null;
    }

    @NotNull
    @Override
    public ItemPath namedSegmentsOnly() {
        return this;
    }

    @NotNull
    @Override
    public ItemPath removeIds() {
        return this;
    }

    @Override
    public QName asSingleName() {
        return this;
    }

    @Override
    public boolean isSingleName() {
        return true;
    }

    @Override
    public ItemName lastName() {
        return this;
    }

    @Override
    public Object last() {
        return this;
    }

    @Override
    public ItemPath firstAsPath() {
        return this;
    }

    @NotNull
    @Override
    public ItemPath allExceptLast() {
        return EMPTY_PATH;
    }

    @Override
    public String toString() {
        if (ItemPath.isObjectReference(this)) {
            return ObjectReferencePathSegment.SYMBOL;
        } else if (ItemPath.isIdentifier(this)) {
            return IdentifierPathSegment.SYMBOL;
        } else if (ItemPath.isParent(this)) {
            return ParentPathSegment.SYMBOL;
        } else {
            return DebugUtil.formatElementName(this);
        }
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(toString());
    }

    @Override
    public ItemPath subPath(int from, int to) {
        if (from > 0) {
            return EMPTY_PATH;
        } else if (to == 0) {
            return EMPTY_PATH;
        } else {
            return this;
        }
    }

    public boolean matches(ItemName other) {
        return QNameUtil.match(this, other);
    }

}
