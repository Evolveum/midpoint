/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class TracingUtil {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    public static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    static String describe(Object object) {
        if (object instanceof Authorization) {
            return ((Authorization) object).getHumanReadableDesc();
        } else {
            return String.valueOf(object); // to be extended if needed
        }
    }

    static <O extends ObjectType> String getObjectTypeName(Class<O> type) {
        return type != null ? type.getSimpleName() : null;
    }
}
