/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathSerialization;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.path.UniformItemPath;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Item path to be provided to LLM. It is similar to {@link ItemPath} but it also contains information about the
 * multivalue-ness of each segment. This is important for LLM to understand the structure of the data.
 * <p>
 * Examples:
 * <p>
 * - `c:givenName`
 * - `c:attributes/icfs:name`
 * - `c:attributes/ri:login`
 * - `c:activation/c:validFrom`
 * - `c:extension/ext1:shoeSize`
 * - `c:email[*]/c:value`
 * <p>
 * The last example means that `c:email` is a multivalued container and this definition refers to its `c:value` property.
 * <p>
 * Individual segments may include an XML namespace prefix, typically "ri:", "icfs:", or "c:".
 * These prefixes are either well-known, or their exact value is not important ("ext1:", "ext2:", and so on).
 */
public class DescriptiveItemPath {

    public record Segment(QName name, boolean isMultiValued) {}

    private final List<Segment> segments;

    private DescriptiveItemPath(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    public static DescriptiveItemPath empty() {
        return new DescriptiveItemPath(Collections.emptyList());
    }

    /**
     * Converts {@link ItemPath} (rooted at item with `rootItemDefinition`) into {@link DescriptiveItemPath}.
     * If the definition is missing, all segments are assumed to be single-valued.
     */
    public static DescriptiveItemPath of(ItemPath itemPath, @Nullable ItemDefinition<?> rootItemDefinition) {
        List<Segment> result = new ArrayList<>();
        ItemDefinition<?> currentDef = rootItemDefinition;

        for (Object segObj : itemPath.getSegments()) {
            if (!ItemPath.isName(segObj)) {
                continue;
            }
            QName qName = ItemPath.toName(segObj);

            boolean multi = false;
            if (currentDef != null) {
                ItemDefinition<?> childDef = currentDef.findItemDefinition(ItemName.fromQName(qName), ItemDefinition.class);
                if (childDef != null) {
                    multi = childDef.isMultiValue();
                    currentDef = childDef;
                } else {
                    currentDef = null;
                }
            }
            result.add(new Segment(qName, multi));
        }
        return new DescriptiveItemPath(result);
    }

    /** Appends a new segment to the path. */
    public DescriptiveItemPath append(QName itemName, boolean isMultiValued) {
        List<Segment> newSegments = new ArrayList<>(segments);
        newSegments.add(new Segment(itemName, isMultiValued));
        return new DescriptiveItemPath(newSegments);
    }

    /** Converts this path back to {@link ItemPath}. */
    public ItemPath getItemPath() {
        List<Object> pathSegments = new ArrayList<>();
        for (Segment segment : segments) {
            pathSegments.add(segment.name());
        }
        return ItemPath.create(pathSegments);
    }

    /** Returns a string representation of this path. */
    public String asString() {
        var ctx = PrismContext.get().getSchemaRegistry().staticNamespaceContext();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (i > 0) {
                sb.append("/");
            }
            String serialized = ItemPathSerialization.serialize(
                    UniformItemPath.from(ItemPath.create(segment.name())), ctx, true)
                    .getXPathWithoutDeclarations();
            sb.append(serialized);
            if (segment.isMultiValued() &&
                    (i != (segments.size()-1))) {
                sb.append("[*]");
            }
        }
        return sb.toString();
    }

    /** A convenience method for quick conversion. Assumes that all segments are single-valued. */
    public static String asStringSimple(ItemPath itemPath) {
        List<Segment> segs = new ArrayList<>();
        for (Object segObj : itemPath.getSegments()) {
            QName name = ItemPath.toName(segObj);
            segs.add(new Segment(name, false));
        }
        return new DescriptiveItemPath(segs).asString();
    }

    @Override
    public String toString() {
        return asString();
    }
}
