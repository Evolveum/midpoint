/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

/**
 * Querydsl "row bean" type related to {@link QUri}.
 */
public class MUri {

    public Integer id;
    public String uri;

    public static MUri of(Integer id, String uri) {
        MUri row = new MUri();
        row.id = id;
        row.uri = uri;
        return row;
    }

    @Override
    public String toString() {
        return "MUri{" +
                "id=" + id +
                ", uri='" + uri + '\'' +
                '}';
    }
}
