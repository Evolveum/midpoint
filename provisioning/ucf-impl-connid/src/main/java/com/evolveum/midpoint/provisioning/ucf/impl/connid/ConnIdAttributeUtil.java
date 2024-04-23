/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeValueCompleteness;

import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

class ConnIdAttributeUtil {

    static boolean isIncomplete(Attribute attribute) {
        // equals() instead of == is needed. The AttributeValueCompleteness enum may be loaded by different classloader
        return AttributeValueCompleteness.INCOMPLETE.equals(attribute.getAttributeValueCompleteness());
    }

    static Object getSingleValue(Attribute attribute) throws SchemaException {
        List<Object> values = attribute.getValue();
        if (values == null || values.isEmpty()) {
            return null;
        } else {
            schemaCheck(values.size() == 1, "Expected single value for %s", attribute.getName());
            return values.get(0); // in theory, this may be null as well
        }
    }
}
