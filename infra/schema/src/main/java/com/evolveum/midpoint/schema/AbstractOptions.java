/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

/**
 * @author semancik
 *
 */
public abstract class AbstractOptions {

    protected void appendFlag(StringBuilder sb, String name, Boolean val) {
        if (val == null) {
            return;
        } else if (val) {
            sb.append(name);
            sb.append(",");
        } else {
            sb.append(name);
            sb.append("=false,");
        }
    }

    protected void appendVal(StringBuilder sb, String name, Object val) {
        if (val != null) {
            sb.append(name);
            sb.append("=");
            sb.append(val);
            sb.append(",");
        }
    }

    protected void removeLastComma(StringBuilder sb) {
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

}
