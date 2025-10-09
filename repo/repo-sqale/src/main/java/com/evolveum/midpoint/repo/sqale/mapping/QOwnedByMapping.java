/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedBy;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Marks mappings for {@link QOwnedBy} entities.
 *
 * @param <S> schema type or the mapped object, typically a container owned by
 * either an object or another container
 * @param <R> row type of the mapped object
 * @param <OR> row type of the owner object
 */
public interface QOwnedByMapping<S, R, OR> {

    /** Returns a row with foreign key fields referencing the provided owner row. */
    R newRowObject(OR ownerRow);

    R insert(S schemaObject, OR ownerRow, JdbcSession jdbcSession) throws SchemaException;
}
