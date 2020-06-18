/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.concepts.SourceLocation;
import com.google.common.base.Strings;

public class RuleErrorMessage {

    private SourceLocation location;
    private String message;

    public SourceLocation location() {
        return location;
    }

    public String message() {
        return message;
    }

    private RuleErrorMessage(SourceLocation location, String message) {
        this.location = location;
        this.message = message;
    }

    public static RuleErrorMessage from(SourceLocation loc, String format, Object... args) {
        return new RuleErrorMessage(loc, Strings.lenientFormat(format, args));
    }

    @Override
    public String toString() {
        return location.toString() + ": " + message;
    }
}
