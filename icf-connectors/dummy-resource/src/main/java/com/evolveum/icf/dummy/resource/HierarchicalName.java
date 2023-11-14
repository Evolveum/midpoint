/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HierarchicalName {

    private final List<String> segments;

    private HierarchicalName(List<String> segments) {
        this.segments = segments;
    }

    public static HierarchicalName of(String name) {
        return new HierarchicalName(
                Arrays.asList(name.split(":")));
    }

    public boolean isTopLevel() {
        return segments.size() == 1;
    }

    public HierarchicalName getParent() {
        if (isTopLevel()) {
            return null;
        }
        return new HierarchicalName(segments.subList(1, segments.size()));
    }

    public String asString() {
        return String.join(":", segments);
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HierarchicalName that = (HierarchicalName) o;
        return Objects.equals(segments, that.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }

    /** Assumes appropriate normalization. */
    boolean residesIn(HierarchicalName ancestor) {
        List<String> ancestorSegments = ancestor.segments;
        if (ancestorSegments.size() >= this.segments.size()) {
            return false;
        }
        for (int iAnc = ancestorSegments.size() - 1, iMine = this.segments.size() - 1; iAnc >= 0; iAnc--, iMine--) {
            if (!segments.get(iMine).equals(ancestorSegments.get(iAnc))) {
                return false;
            }
        }
        return true;
    }

    /** Assumes appropriate normalization + that the object has the old ancestor. */
    public HierarchicalName move(HierarchicalName oldAncestorHName, HierarchicalName newAncestorHName) {
        List<String> prefix = segments.subList(0, segments.size() - oldAncestorHName.segments.size());
        List<String> newSegments = new ArrayList<>(prefix);
        newSegments.addAll(newAncestorHName.segments);
        return new HierarchicalName(newSegments);
    }
}
