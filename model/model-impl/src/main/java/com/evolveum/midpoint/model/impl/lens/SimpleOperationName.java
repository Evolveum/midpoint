/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

/**
 * @author semancik
 *
 */
public enum SimpleOperationName {

    ADD("add", "added"),
    MODIFY("modify", "modified"),
    DELETE("delete", "deleted");

    private final String value;
    private final String pastTense;

    SimpleOperationName(String value, String pastTense) {
        this.value = value;
        this.pastTense = pastTense;
    }

    public String getValue() {
        return value;
    }

    public String getPastTense() {
        return pastTense;
    }
}
