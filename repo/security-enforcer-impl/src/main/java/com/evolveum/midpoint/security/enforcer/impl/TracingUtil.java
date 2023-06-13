/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

public class TracingUtil {

    static final String START = ">";
    static final String END = "=";
    static final String CONT = "|";

    static final String SEC_START = "SEC>";
    static final String SEC_END = "SEC=";
    static final String OP_START = " ".repeat(1) + "OP>";
    static final String OP_END = " ".repeat(1) + "OP=";
    static final String OP = " ".repeat(1) + "OP|";
    static final String AUTZ = " ".repeat(2) + "AUTZ.";
    static final String SELECTORS = " ".repeat(6) + "SELECTORS:";
    static final String SEL = " ".repeat(6) + "SEL.";

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    public static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    static String describe(Object object) {
        if (object instanceof Authorization) {
            return ((Authorization) object).getHumanReadableDesc();
        } else if (object instanceof ObjectSelectorType) {
            return getHumanReadableDesc((ObjectSelectorType) object);
        } else {
            return String.valueOf(object); // to be extended if needed
        }
    }

    static String getTypeName(Class<?> type) {
        return type != null ? type.getSimpleName() : null;
    }

    public static String getHumanReadableDesc(ValueSelector selector) {
        return getHumanReadableDesc(selector.getBean()); // FIXME temporary
    }

    public static String getHumanReadableDesc(ObjectSelectorType selector) {
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
            return sb.toString();
        }
    }
}
