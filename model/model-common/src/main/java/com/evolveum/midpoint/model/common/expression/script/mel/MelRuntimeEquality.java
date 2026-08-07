/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel;

import dev.cel.common.CelOptions;
import dev.cel.runtime.RuntimeEquality;

public class MelRuntimeEquality extends RuntimeEquality {

    public MelRuntimeEquality(CelOptions celOptions) {
        super(celOptions);
    }

    @Override
    public boolean objectEquals(Object x, Object y) {
        if (x == y) {
            return true;
        }
        Object nx = x;
        if (CelTypeMapper.isCelNull(x)) {
            nx = null;
        }
        Object ny = y;
        if (CelTypeMapper.isCelNull(y)) {
            ny = null;
        }

        if (x instanceof MelComparable xc) {
            return xc.melEquals(ny);
        }

        if (y instanceof MelComparable yc) {
            return yc.melEquals(nx);
        }

        return super.objectEquals(x, y);
    }
}
