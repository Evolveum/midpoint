/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
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
