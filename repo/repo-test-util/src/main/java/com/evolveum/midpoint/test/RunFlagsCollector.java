/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Used to collect information about whether particular code (e.g., a policy constraint) was run or not.
 */
public class RunFlagsCollector {

    private final String name;

    /** Intentionally a list, to preserve order and information about multiple invocations. */
    private final List<String> flags = new ArrayList<>();

    public RunFlagsCollector() {
        this("unnamed");
    }

    public RunFlagsCollector(String name) {
        this.name = name;
    }

    public void add(String flag) {
        flags.add(flag);
    }

    public void clear() {
        flags.clear();
    }

    public void assertPresent(String flag) {
        assertThat(flags)
                .withFailMessage("'%s' was not run even if it should; in %s. Present: %s", flag, name, flags)
                .contains(flag);
    }

    public void assertNotPresent(String flag) {
        assertThat(flags)
                .withFailMessage("'%s' was run even if it should not; in %s. Present: %s", flag, name, flags)
                .doesNotContain(flag);
    }

    @Override
    public String toString() {
        return "RunFlagsCollector{" +
                "name='" + name + '\'' +
                ", flags=" + flags +
                '}';
    }
}
