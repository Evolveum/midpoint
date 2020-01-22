/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
