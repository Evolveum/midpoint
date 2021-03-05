/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.operations;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sqale.SqaleTransformerContext;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class AddObjectOperation<S extends ObjectType, Q extends QObject<R>, R extends MObject> {

    private final PrismObject<S> object;
    private final RepoAddOptions options;
    private final OperationResult result;

    private SqlRepoContext sqlRepoContext;
    private Q root;
    private ObjectSqlTransformer<S, Q, R> transformer;

    public AddObjectOperation(@NotNull PrismObject<S> object,
            @NotNull RepoAddOptions options, @NotNull OperationResult result) {
        this.object = object;
        this.options = options;
        this.result = result;
    }

    /** Inserts the object provided to the constructor and returns its OID. */
    public String execute(SqaleTransformerContext transformerContext) throws SchemaException {
        // TODO utilize options and result
        sqlRepoContext = transformerContext.sqlRepoContext();
        SqaleModelMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(object.getCompileTimeClass());
        root = rootMapping.defaultAlias();
        transformer = (ObjectSqlTransformer<S, Q, R>)
                rootMapping.createTransformer(transformerContext);

        if (options.isOverwrite()) {
            return overwriteObject();
        } else if (object.getOid() != null) {
            return addObjectWithOid();
        } else {
            return addObjectWithoutOid();
        }
    }

    private String overwriteObject() {
        throw new UnsupportedOperationException(); // TODO
    }

    private String addObjectWithOid() throws SchemaException {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            R row = transformer.toRowObjectWithoutFullObject(object.asObjectable(), jdbcSession);
            transformer.setFullObject(row, object.asObjectable());
            UUID oid = jdbcSession.newInsert(root)
                    // default populate mapper ignores null, that's good, especially for objectType
                    .populate(row)
                    .executeWithKey(root.oid);
            return Objects.requireNonNull(oid, "OID of inserted object can't be null")
                    .toString();
        }
    }

    private String addObjectWithoutOid() throws SchemaException {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            R row = transformer.toRowObjectWithoutFullObject(object.asObjectable(), jdbcSession);
            // first insert without full object, because we don't know the OID yet
            UUID oid = jdbcSession.newInsert(root)
                    // default populate mapper ignores null, that's good, especially for objectType
                    .populate(row)
                    .executeWithKey(root.oid);
            String oidString =
                    Objects.requireNonNull(oid, "OID of inserted object can't be null")
                            .toString();
            object.setOid(oidString);

            // now to update full object with known OID
            transformer.setFullObject(row, object.asObjectable());
            jdbcSession.newUpdate(root)
                    .set(root.fullObject, row.fullObject)
                    .where(root.oid.eq(oid))
                    .execute();

            return oidString;
        }
    }
}
