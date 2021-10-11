/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface DebugDumpable {

    String INDENT_STRING = "  ";

    /**
     * Show the content of the object intended for diagnostics by system administrator. The out
     * put should be suitable to use in system logs at "debug" level. It may be multi-line, but in
     * that case it should be well indented and quite terse.
     *
     * As it is intended to be used by system administrator, it should not use any developer terms
     * such as class names, exceptions or stack traces.
     *
     * @return content of the object intended for diagnostics by system administrator.
     */
    default String debugDump() {
        return debugDump(0);
    }

    String debugDump(int indent);

    default Object debugDumpLazily() {
        return DebugUtil.debugDumpLazily(this);
    }

    default Object debugDumpLazily(int indent) {
        return DebugUtil.debugDumpLazily(this, indent);
    }
}
