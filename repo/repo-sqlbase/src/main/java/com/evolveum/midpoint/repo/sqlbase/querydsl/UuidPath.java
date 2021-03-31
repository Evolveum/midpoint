/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.sql.Types;
import java.util.UUID;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.ComparablePath;

public class UuidPath extends ComparablePath<UUID> {

    private static final long serialVersionUID = -7475296682846579579L;

    /**
     * Alias for {@link Types#OTHER} working in tandem with {@link UuidPath}.
     * This is important especially for setting NULLs explicitly, OTHER works, JAVA_OBJECT not.
     * Reasons for this are deep in PostgreSQL JDBC driver.
     */
    public static final int UUID_TYPE = Types.OTHER;

    public UuidPath(PathMetadata metadata) {
        super(UUID.class, metadata);
    }
}
