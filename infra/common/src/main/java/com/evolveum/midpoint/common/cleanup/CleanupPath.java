/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Class that defines schema type (using {@link QName}). item path and action that should be used during cleanup operation
 */
public class CleanupPath implements Serializable, Comparable<CleanupPath> {

    private QName type;

    private ItemPath path;

    private CleanupPathAction action;

    @SuppressWarnings("unused")
    public CleanupPath() {
    }

    public CleanupPath(QName type, ItemPath path, CleanupPathAction action) {
        this.type = type;
        this.path = path;
        this.action = action;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public ItemPath getPath() {
        return path;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public CleanupPathAction getAction() {
        return action;
    }

    public void setAction(CleanupPathAction action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        CleanupPath that = (CleanupPath) o;
        return type == that.type && Objects.equals(path, that.path) && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, path, action);
    }

    private String getTypeAsString() {
        return type != null ? QNameUtil.qNameToUri(type) : null;
    }

    private String getPathAsString() {
        return path != null ? path.toString() : null;
    }

    @Override
    public int compareTo(@NotNull CleanupPath o) {
        return Comparator.nullsLast(Comparator.comparing(CleanupPath::getTypeAsString))
                .thenComparing(Comparator.nullsLast(Comparator.comparing(CleanupPath::getPathAsString)))
                .thenComparing(Comparator.nullsLast(Comparator.comparing(CleanupPath::getAction)))
                .compare(this, o);
    }

    public CleanupPath copy() {
        return new CleanupPath(type, path, action);
    }
}
