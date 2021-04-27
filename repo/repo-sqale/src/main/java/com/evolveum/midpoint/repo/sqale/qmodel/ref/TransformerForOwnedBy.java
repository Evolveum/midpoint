/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

/**
 * Declares capability to insert row for the provided schema object owned by provided row.
 *
 * @param <S> schema type
 * @param <R> target type of the transformation, a row bean
 * @param <OR> row type of the reference owner
 */
public interface TransformerForOwnedBy<S, R, OR> {

    /** Contract for insertion of row of type {@link R} owned by {@link OR}. */
    R insert(S schemaObject, OR ownerRow, JdbcSession jdbcSession);
}
