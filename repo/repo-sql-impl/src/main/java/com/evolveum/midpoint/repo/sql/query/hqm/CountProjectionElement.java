/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import org.jetbrains.annotations.NotNull;

public class CountProjectionElement extends ProjectionElement {

    @NotNull private final String item;
    private final boolean distinct;

    public CountProjectionElement(@NotNull String item, boolean distinct) {
        this.item = item;
        this.distinct = distinct;
    }

    protected void dumpToHql(StringBuilder sb) {
        sb.append("count(");
        if (distinct) {
            sb.append("distinct ");
        }
        sb.append(item).append(")");
    }
}
