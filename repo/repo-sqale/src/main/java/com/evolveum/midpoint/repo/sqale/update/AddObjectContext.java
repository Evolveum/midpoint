/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import java.util.Objects;
import java.util.UUID;

import com.querydsl.core.QueryException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sqale.ContainerValueIdGenerator;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Add object operation context; used only for true add, not overwrite which is more like modify.
 */
public class AddObjectContext<S extends ObjectType, Q extends QObject<R>, R extends MObject> {

    private final SqaleRepoContext repositoryContext;
    private final PrismObject<S> object;
    private final RepoAddOptions options;
    private final OperationResult result;

    private Q root;
    private QObjectMapping<S, Q, R> rootMapping;
    private MObjectType objectType;

    public AddObjectContext(
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull PrismObject<S> object,
            @NotNull RepoAddOptions options,
            @NotNull OperationResult result) {
        this.repositoryContext = repositoryContext;
        this.object = object;
        this.options = options;
        this.result = result;
    }

    /**
     * Inserts the object provided to the constructor and returns its OID.
     */
    public String execute()
            throws SchemaException, ObjectAlreadyExistsException {
        try {
            // TODO utilize options and result
            object.setVersion("1"); // initial add always uses 1 as version number
            Class<S> schemaObjectClass = object.getCompileTimeClass();
            objectType = MObjectType.fromSchemaType(schemaObjectClass);
            rootMapping = repositoryContext.getMappingBySchemaType(schemaObjectClass);
            root = rootMapping.defaultAlias();

            if (object.getOid() == null) {
                return addObjectWithoutOid();
            } else {
                // this also handles overwrite after ObjectNotFoundException
                return addObjectWithOid();
            }
        } catch (QueryException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PSQLException) {
                handlePostgresException((PSQLException) cause);
            }
            throw e;
        }
    }

    private String addObjectWithOid() throws SchemaException {
        long lastCid = new ContainerValueIdGenerator().generateForNewObject(object);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            S schemaObject = object.asObjectable();
            R row = rootMapping.toRowObjectWithoutFullObject(schemaObject, jdbcSession);
            row.containerIdSeq = lastCid + 1;
            rootMapping.setFullObject(row, schemaObject);

            UUID oid = jdbcSession.newInsert(root)
                    // default populate mapper ignores null, that's good, especially for objectType
                    .populate(row)
                    .executeWithKey(root.oid);

            row.objectType = objectType; // sub-entities can use it, now it's safe to set it
            rootMapping.storeRelatedEntities(row, schemaObject, jdbcSession);

            jdbcSession.commit();
            return Objects.requireNonNull(oid, "OID of inserted object can't be null")
                    .toString();
        }
    }

    private String addObjectWithoutOid() throws SchemaException {
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            S schemaObject = object.asObjectable();
            R row = rootMapping.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

            // first insert without full object, because we don't know the OID yet
            UUID oid = jdbcSession.newInsert(root)
                    // default populate mapper ignores null, that's good, especially for objectType
                    .populate(row)
                    .executeWithKey(root.oid);
            String oidString =
                    Objects.requireNonNull(oid, "OID of inserted object can't be null")
                            .toString();
            object.setOid(oidString);

            long lastCid = new ContainerValueIdGenerator().generateForNewObject(object);

            // now to update full object with known OID
            rootMapping.setFullObject(row, schemaObject);
            jdbcSession.newUpdate(root)
                    .set(root.fullObject, row.fullObject)
                    .set(root.containerIdSeq, lastCid + 1)
                    .where(root.oid.eq(oid))
                    .execute();

            row.oid = oid;
            row.objectType = objectType; // sub-entities can use it, now it's safe to set it
            rootMapping.storeRelatedEntities(row, schemaObject, jdbcSession);

            jdbcSession.commit();
            return oidString;
        }
    }

    // TODO can be static. Should it move to other SQL exception handling code?

    /** Throws more specific exception or returns and then original exception should be rethrown. */
    private void handlePostgresException(PSQLException psqlException)
            throws ObjectAlreadyExistsException {
        String state = psqlException.getSQLState();
        String message = psqlException.getMessage();
        if (PSQLState.UNIQUE_VIOLATION.getState().equals(state)) {
            if (message.contains("m_object_oid_pkey")) {
                String oid = StringUtils.substringBetween(message, "(oid)=(", ")");
                throw new ObjectAlreadyExistsException(
                        oid != null ? "Provided OID " + oid + " already exists" : message,
                        psqlException);
            }
        }
    }
}
