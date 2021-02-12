/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 */
public class ObjectSelector implements Serializable, ShortDumpable {

    private final UniformItemPath path; // do not change to ItemPath unless equals/hashCode is adapted

    public ObjectSelector(UniformItemPath path) {
        super();
        this.path = path;
    }

    public UniformItemPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "ObjectSelector(" + path + ")";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjectSelector)) {
            return false;
        }

        ObjectSelector that = (ObjectSelector) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
