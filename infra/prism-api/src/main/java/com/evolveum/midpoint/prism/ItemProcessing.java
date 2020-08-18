/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

/**
 * @author semancik
 *
 */
public enum ItemProcessing {

    IGNORE("ignore"), MINIMAL("minimal"), AUTO("auto"), FULL("full");

    private final String stringValue;

    ItemProcessing(final String value) {
        this.stringValue = value;
    }

    public String getValue() {
        return stringValue;
    }

    public static ItemProcessing findByValue(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        for (ItemProcessing val : ItemProcessing.values()) {
            if (val.getValue().equals(stringValue)) {
                return val;
            }
        }
        return null;
    }

}
