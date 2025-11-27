/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

class SchemaSerializer {

    /**
     * Remembers mapping from stringified {@link DescriptiveItemPath} to real {@link ItemPath}.
     *
     * It is here because we cannot accurately convert text representation of {@link DescriptiveItemPath}
     * to {@link ItemPath} without knowing the exact extension namespaces.
     */
    protected Map<String, ItemPath> descriptiveToItemPath = new HashMap<>();

    private static final List<QName> SUPPORTED_TYPE_NAMES = List.of(
            DOMUtil.XSD_STRING,
            DOMUtil.XSD_BOOLEAN,
            DOMUtil.XSD_INT,
            DOMUtil.XSD_LONG,
            DOMUtil.XSD_DOUBLE,
            DOMUtil.XSD_FLOAT,
            DOMUtil.XSD_DATETIME);

    // Ugly hack - to be discussed
    QName fixTypeName(@NotNull QName original) {
        if (QNameUtil.contains(SUPPORTED_TYPE_NAMES, original)) {
            return original;
        } else {
            return DOMUtil.XSD_STRING;
        }
    }

    void registerPathMapping(String descriptivePathString, ItemPath itemPath) {
        descriptiveToItemPath.put(descriptivePathString, itemPath);
    }

    /** Throws an exception if the path is unknown. */
    ItemPath getItemPath(String descriptivePath) {
        return MiscUtil.stateNonNull(
                descriptiveToItemPath.get(descriptivePath),
                "No ItemPath mapping for DescriptiveItemPath '%s'",
                descriptivePath);
    }
}
