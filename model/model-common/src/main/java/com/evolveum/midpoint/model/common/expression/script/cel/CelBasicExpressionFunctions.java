/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import dev.cel.common.types.CelType;
import dev.cel.common.values.CelValue;

public class CelBasicExpressionFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(CelBasicExpressionFunctions.class);

    private final BasicExpressionFunctions implementation;

    public CelBasicExpressionFunctions(BasicExpressionFunctions implementation) {
        this.implementation = implementation;
    }

    public String lc(String orig) {
        return BasicExpressionFunctions.lc(orig);
    }

    public String uc(String orig) {
        return BasicExpressionFunctions.uc(orig);
    }

    public boolean contains(Object object, Object search) {
        return implementation.contains(object, search);
    }

    public boolean containsIgnoreCase(Object object, Object search) {
        return implementation.containsIgnoreCase(object, search);
    }

    public String trim(String orig) {
        return BasicExpressionFunctions.trim(orig);
    }

    public String norm(Object orig) {
        if (orig == null) {
            return null;
        }
        if (orig instanceof PolyString) {
            return implementation.norm((PolyString)orig);
        }
        if (orig instanceof String) {
            return implementation.norm((String)orig);
        }
        return implementation.norm(orig.toString());
    }

    public String stringify(Object whatever) {
        LOGGER.info("SSSSSS: stringify {}", whatever);
        return implementation.stringify(whatever);
    }
}
