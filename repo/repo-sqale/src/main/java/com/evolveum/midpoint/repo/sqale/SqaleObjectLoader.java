/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.Collection;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Description of internal read-by-OID operation in the context of existing JDBC session.
 * This is needed for enriching of some containers that need to read their owner objects.
 */
public interface SqaleObjectLoader {

    <S extends ObjectType> S readByOid(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException;
}
