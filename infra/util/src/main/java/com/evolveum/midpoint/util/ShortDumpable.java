/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface ShortDumpable {

    /**
     * Show the content of the object intended for diagnostics. This method is supposed
     * to append a compact, human-readable output in a single line. Unlike toString() method,
     * there is no requirement to identify the actual class or type of the object.
     * It is assumed that the class/type will be obvious from the context in which the
     * output is used.
     *
     * @param sb StringBuilder to which to a compact one-line content of the object intended
     *           for diagnostics by system administrator should be appended.
     */
    void shortDump(StringBuilder sb);

    // convenience version
    default String shortDump() {
        StringBuilder sb = new StringBuilder();
        shortDump(sb);
        return sb.toString();
    }

    default Object shortDumpLazily() {
        return DebugUtil.shortDumpLazily(this);
    }
}
