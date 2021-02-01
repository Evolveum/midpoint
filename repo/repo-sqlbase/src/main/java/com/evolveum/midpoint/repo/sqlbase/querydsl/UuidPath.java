/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.util.UUID;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.ComparablePath;

public class UuidPath extends ComparablePath<UUID> {

    private static final long serialVersionUID = -7475296682846579579L;

    protected UuidPath(Class<? extends UUID> type, PathMetadata metadata) {
        super(type, metadata);
    }
}
