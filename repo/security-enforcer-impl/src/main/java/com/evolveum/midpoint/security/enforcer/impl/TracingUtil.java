/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

public class TracingUtil {

    static final String START = ">";
    static final String END = "=";
    static final String CONT = "|";

    // TODO resolve spacing somehow
    static final String SEC = "SEC";
    static final String PARTIAL_SEC_SPACE = " ".repeat(1);
    static final String AUTZ_SPACE = " ".repeat(2);
    static final String SEL_SPACE = " ".repeat(4);
    static final String INTERIOR_SPACE = "  ";

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    public static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    static String getTypeName(Class<?> type) {
        return type != null ? type.getSimpleName() : null;
    }

    static String getHumanReadableDesc(ValueSelector selector) {
        return getHumanReadableDesc(selector.getBean()); // FIXME temporary
    }

    private static String getHumanReadableDesc(ObjectSelectorType selector) {
        if (selector == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("selector");
            Long id = selector.getId();
            if (id != null) {
                sb.append(" #").append(id);
            }
            String name = selector.getName();
            if (name != null) {
                sb.append(" '").append(name).append("'");
            }
            var type = selector.getType();
            if (type != null) {
                sb.append(" (type: ").append(type.getLocalPart()).append(")");
            }
            return sb.toString();
        }
    }
}
