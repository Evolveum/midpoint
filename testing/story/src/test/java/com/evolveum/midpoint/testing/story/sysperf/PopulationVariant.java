/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

enum PopulationVariant {

    P10("10", 10),
    P100("100", 100),
    P1K("1k", 1000),
    P10K("10k", 10000),
    P100K("100k", 100_000),
    P1M("1m", 1_000_000);

    private static final String PROP_POPULATION = "population";

    private final String name;
    private final int accounts;

    PopulationVariant(String name, int accounts) {
        this.name = name;
        this.accounts = accounts;
    }

    public String getName() {
        return name;
    }

    public int getAccounts() {
        return accounts;
    }

    public static PopulationVariant setup() {
        String configuredName = System.getProperty(PROP_POPULATION, P10.name);
        PopulationVariant variant = fromName(configuredName);
        System.out.println("Population variant: " + variant);
        return variant;
    }

    private static PopulationVariant fromName(String name) {
        for (PopulationVariant value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown population variant: " + name);
    }
}
