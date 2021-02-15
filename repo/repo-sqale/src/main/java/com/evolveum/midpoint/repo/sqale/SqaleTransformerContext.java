/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.schema.SchemaHelper;

/**
 * Extension of {@link SqlTransformerContext} adding Sqale features like {@link UriCache} support.
 */
public class SqaleTransformerContext extends SqlTransformerContext {

    public SqaleTransformerContext(SchemaHelper schemaService, SqaleRepoContext sqaleRepoContext) {
        super(schemaService, sqaleRepoContext);
    }

    private SqaleRepoContext sqaleRepoContext() {
        return (SqaleRepoContext) sqlRepoContext;
    }

    /** Returns ID for cached URI without going ot database. */
    public Integer resolveToId(String uri) {
        return sqaleRepoContext().resolveToId(uri);
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    public Integer processCachedUri(String uri, JdbcSession jdbcSession) {
        return sqaleRepoContext().processCachedUri(uri, jdbcSession);
    }
}

