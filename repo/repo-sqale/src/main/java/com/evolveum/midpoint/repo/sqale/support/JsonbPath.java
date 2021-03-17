/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.support;

import java.sql.Types;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.SimplePath;

public class JsonbPath extends SimplePath<Jsonb> {

    private static final long serialVersionUID = -5018414609329370755L;

    /**
     * Alias for {@link Types#OTHER} working in tandem with {@link JsonbPath}.
     * This is important especially for setting NULLs explicitly, OTHER works, JAVA_OBJECT not.
     * Reasons for this are deep in PostgreSQL JDBC driver.
     */
    public static final int JSONB_TYPE = Types.OTHER;

    public JsonbPath(PathMetadata metadata) {
        super(Jsonb.class, metadata);
    }
}
