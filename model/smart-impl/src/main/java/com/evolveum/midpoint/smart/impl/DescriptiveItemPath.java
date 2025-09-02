/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Item path to be provided to LLM. It is similar to {@link ItemPath} but it also contains information about the
 * multivalue-ness of each segment. This is important for LLM to understand the structure of the data.
 *
 * Examples:
 *
 * - `c:givenName`
 * - `c:attributes/icfs:name`
 * - `c:attributes/ri:login`
 * - `c:activation/c:validFrom`
 * - `c:extension/ext1:shoeSize`
 * - `c:email[*]/c:value`
 *
 * The last example means that `c:email` is a multivalued container and this definition refers to its `c:value` property.
 *
 * Individual segments may include an XML namespace prefix, typically "ri:", "icfs:", or "c:".
 * These prefixes are either well-known, or their exact value is not important ("ext1:", "ext2:", and so on).
 */
public class DescriptiveItemPath {

    public static DescriptiveItemPath empty() {
        return new DescriptiveItemPath();
    }

    /**
     * Converts {@link ItemPath} (rooted at item with `rootItemDefinition`) into {@link DescriptiveItemPath}.
     * If the definition is missing, all segments are assumed to be single-valued.
     */
    public static DescriptiveItemPath of(ItemPath itemPath, @Nullable ItemDefinition<?> rootItemDefinition) {
        throw new UnsupportedOperationException("MID-10840");
    }

    /** Appends a new segment to the path. */
    public DescriptiveItemPath append(QName itemName, boolean isMultivalued) {
        throw new UnsupportedOperationException("MID-10840");
    }

    /** Converts this path back to {@link ItemPath}. */
    public ItemPath getItemPath() {
        throw new UnsupportedOperationException("MID-10840");
    }

    /** Returns a string representation of this path. */
    public String asString() {
        throw new UnsupportedOperationException("MID-10840");
    }

    /** A convenience method for quick conversion. Assumes that all segments are single-valued. */
    public static String asStringSimple(ItemPath itemPath) {
        return DescriptiveItemPath.of(itemPath, null).asString();
    }
}
