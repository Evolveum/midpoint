/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

class SchemaSerializer {

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
}
