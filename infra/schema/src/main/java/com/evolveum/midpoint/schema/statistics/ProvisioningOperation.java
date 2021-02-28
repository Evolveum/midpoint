/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * TODO reconsider usefulness of this class
 */
public enum ProvisioningOperation {

    ICF_GET("get"), ICF_SEARCH("search"),

    ICF_CREATE("create"), ICF_UPDATE("update"), ICF_DELETE("delete"),

    ICF_SYNC("sync"),

    ICF_SCRIPT("script"),

    ICF_GET_LATEST_SYNC_TOKEN("getLatestSyncToken"), ICF_GET_SCHEMA("getSchema");

    @NotNull private final String name;

    ProvisioningOperation(@NotNull String name) {
        this.name = name;
    }

    public static ProvisioningOperation find(String name) {
        return Arrays.stream(values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public String getName() {
        return name;
    }
}
