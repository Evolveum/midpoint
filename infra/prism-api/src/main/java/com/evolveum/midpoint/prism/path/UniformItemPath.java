/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.util.ShortDumpable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public interface UniformItemPath extends Serializable, Cloneable, ShortDumpable, ItemPath {

    @NotNull
    List<ItemPathSegment> getSegments();

    ItemPathSegment first();

    @NotNull
    default UniformItemPath rest() {
        return rest(1);
    }

    @Nullable
    ItemPathSegment last();

    /**
     * Returns first segment in a form of path.
     */
    UniformItemPath firstAsPath();

    @NotNull
    UniformItemPath rest(int n);

    /**
     * Returns a path containing all segments except the last one.
     */
    @NotNull
    UniformItemPath allExceptLast();

    /**
     * Returns a path containing all segments up to (and not including) the last one.
     */
    @NotNull
    UniformItemPath allUpToLastName();

    UniformItemPath allUpToIncluding(int i);

    /**
     * Makes the path "normal" by inserting null Id segments where they were omitted.
     */
    UniformItemPath normalize();

    @NotNull
    UniformItemPath removeIds();

    @NotNull
    UniformItemPath namedSegmentsOnly();

    @NotNull
    UniformItemPath stripVariableSegment();

    @NotNull
    UniformItemPath append(Object... components);

    UniformItemPath remainder(ItemPath prefix);

    /**
     * More strict version of ItemPath comparison. Does not use any normalization
     * nor approximate matching QNames via QNameUtil.match.
     *
     * For semantic-level comparison, please use equivalent(..) method.
     */
    @Override
    boolean equals(Object obj);

    UniformItemPath clone();

    // TEMPORARY TYPED AS OBJECT (TODO FIXME)
    Object asItemPathType();

    ItemPathSegment getSegment(int i);

    void setNamespaceMap(Map<String, String> namespaceMap);

    Map<String, String> getNamespaceMap();

    static UniformItemPath empty() {
        return UniformItemPathImpl.EMPTY_PATH;
    }

    static UniformItemPath create(Object... segments) {
        return UniformItemPathImpl.create(segments);
    }

    static @NotNull UniformItemPath from(ItemPath path) {
        return UniformItemPathImpl.fromItemPath(path);
    }

    static ItemPathSegment createSegment(QName qname, boolean variable) {
        if (ParentPathSegment.QNAME.equals(qname)) {
            return new ParentPathSegment();
        } else if (ObjectReferencePathSegment.QNAME.equals(qname)) {
            return new ObjectReferencePathSegment();
        } else if (IdentifierPathSegment.QNAME.equals(qname)) {
            return new IdentifierPathSegment();
        } else if (variable) {
            return new VariableItemPathSegment(qname);
        } else {
            return new NameItemPathSegment(qname);
        }
    }
}
