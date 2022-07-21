/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryObjectDiagnosticData;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.RepositoryMappingException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 *
 * @param <S> schema type of the object
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QObjectMapping<S extends ObjectType, Q extends QObject<R>, R extends MObject>
        extends SqaleTableMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "o";

    private static QObjectMapping<?, ?, ?> instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectMapping<?, ?, ?> initObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QObjectMapping<>(
                QObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectType.class, QObject.CLASS,
                repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectMapping<?, ?, ?> getObjectMapping() {
        return Objects.requireNonNull(instance);
    }

    protected QObjectMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);

        addItemMapping(PrismConstants.T_ID, uuidMapper(q -> q.oid));
        addItemMapping(F_NAME, polyStringMapper(
                q -> q.nameOrig, q -> q.nameNorm));
        addRefMapping(F_TENANT_REF,
                q -> q.tenantRefTargetOid,
                q -> q.tenantRefTargetType,
                q -> q.tenantRefRelationId,
                QOrgMapping::getOrgMapping);
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(q -> q.lifecycleState));
        // version/cidSeq is not mapped for queries or deltas, it's managed by repo explicitly

        addItemMapping(F_POLICY_SITUATION, multiUriMapper(q -> q.policySituations));
        addItemMapping(F_SUBTYPE, multiStringMapper(q -> q.subtypes));
        // full-text is not item mapping, but filter on the whole object
        addExtensionMapping(F_EXTENSION, MExtItemHolderType.EXTENSION, q -> q.ext);

        addNestedMapping(F_METADATA, MetadataType.class)
                .addRefMapping(MetadataType.F_CREATOR_REF,
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addRefMapping(MetadataType.F_MODIFIER_REF,
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(MetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp))
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        QObjectReferenceMapping.initForCreateApprover(repositoryContext))
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        QObjectReferenceMapping.initForModifyApprover(repositoryContext));

        addRefMapping(F_PARENT_ORG_REF,
                QObjectReferenceMapping.initForParentOrg(repositoryContext));

        addContainerTableMapping(F_OPERATION_EXECUTION,
                QOperationExecutionMapping.init(repositoryContext),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid)));
        addContainerTableMapping(F_TRIGGER,
                QTriggerMapping.init(repositoryContext),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid)));
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        // TODO: there is currently no support for index-only extensions (from entity.ext).
        //  See how QShadowMapping.loadIndexOnly() is used, and probably compose the result of this call
        //  using super... call in the subclasses. (joining arrays? providing mutable list?)
        return new Path[] { entity.oid, entity.fullObject };
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QObject<>(MObject.class, alias);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MObject();
    }

    // region transformation
    @Override
    public S toSchemaObject(Tuple row, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        byte[] fullObject = Objects.requireNonNull(row.get(entityPath.fullObject));
        UUID oid = Objects.requireNonNull(row.get(entityPath.oid));
        S ret = parseSchemaObject(fullObject, oid.toString());
        if (GetOperationOptions.isAttachDiagData(SelectorOptions.findRootOptions(options))) {
            RepositoryObjectDiagnosticData diagData = new RepositoryObjectDiagnosticData(fullObject.length);
            ret.asPrismContainer().setUserData(RepositoryService.KEY_DIAG_DATA, diagData);
        }
        return ret;
    }

    /**
     * The same function as in overridden method, but softer exception handling.
     * This targets cases like {@link RepositoryService#searchObjects} where single wrong object
     * should not spoil the whole result list.
     */
    @Override
    public S toSchemaObjectSafe(
            Tuple tuple,
            Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession,
            boolean forceFull) {
        try {
            return toSchemaObjectWithResolvedNames(tuple, entityPath, options, jdbcSession, forceFull);
        } catch (SchemaException e) {
            try {
                PrismObject<S> errorObject = prismContext().createObject(schemaType());
                //noinspection ConstantConditions - this must not be null, the column is not
                String oid = tuple.get(entityPath.oid).toString();
                errorObject.setOid(oid);
                errorObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                logger.warn("Unreadable object with OID {}, reason: {}\n"
                        + "Surrogate object with error message as a name will be used.", oid, e.toString());
                return errorObject.asObjectable();
            } catch (SchemaException ex) {
                throw new RepositoryMappingException("Schema exception [" + ex + "] while handling schema exception: " + e, e);
            }
        }
    }

    /**
     * Override this to fill additional row attributes after calling this super version.
     *
     * *This must be called with active JDBC session* so it can create new {@link QUri} rows.
     * As this is intended for inserts *DO NOT* set {@link MObject#objectType} to any value,
     * it must be NULL otherwise the DB will complain about the value for the generated column.
     *
     * OID may be null, hence the method does NOT create any sub-entities, see
     * {@link #storeRelatedEntities(MObject, ObjectType, JdbcSession)}.
     * Try to keep order of fields here, in M-class (MObject for this one) and in SQL the same.
     */
    @SuppressWarnings("DuplicatedCode") // see comment for metadata lower
    @NotNull
    public R toRowObjectWithoutFullObject(S schemaObject, JdbcSession jdbcSession) {
        R row = newRowObject();

        row.oid = SqaleUtils.oidToUUid(schemaObject.getOid());
        // objectType MUST be left NULL for INSERT, it's determined by PG
        setPolyString(schemaObject.getName(), o -> row.nameOrig = o, n -> row.nameNorm = n);
        // fullObject is managed outside this method
        setReference(schemaObject.getTenantRef(),
                o -> row.tenantRefTargetOid = o,
                t -> row.tenantRefTargetType = t,
                r -> row.tenantRefRelationId = r);
        row.lifecycleState = schemaObject.getLifecycleState();
        // containerIdSeq is managed outside this method
        row.version = SqaleUtils.objectVersionAsInt(schemaObject);

        // complex DB fields
        row.policySituations = processCacheableUris(schemaObject.getPolicySituation());
        row.subtypes = stringsToArray(schemaObject.getSubtype());
        row.fullTextInfo = repositoryContext().fullTextIndex(schemaObject);
        row.ext = processExtensions(schemaObject.getExtension(), MExtItemHolderType.EXTENSION);

        // This is duplicate code with QAssignmentMapping.insert, but making interface
        // and needed setters (fields are not "interface-able") would create much more code.
        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            setReference(metadata.getCreatorRef(),
                    o -> row.creatorRefTargetOid = o,
                    t -> row.creatorRefTargetType = t,
                    r -> row.creatorRefRelationId = r);
            row.createChannelId = processCacheableUri(metadata.getCreateChannel());
            row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

            setReference(metadata.getModifierRef(),
                    o -> row.modifierRefTargetOid = o,
                    t -> row.modifierRefTargetType = t,
                    r -> row.modifierRefRelationId = r);
            row.modifyChannelId = processCacheableUri(metadata.getModifyChannel());
            row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
        }
        return row;
    }

    /**
     * Stores other entities related to the main object row like containers, references, etc.
     * This is not part of {@link #toRowObjectWithoutFullObject} because it requires know OID
     * which is not assured before calling that method.
     *
     * *Always call this super method first in overriding methods.*
     *
     * @param row master row for the added object("aggregate root")
     * @param schemaObject schema objects for which the details are stored
     * @param jdbcSession JDBC session used to insert related rows
     */
    public void storeRelatedEntities(
            @NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        Objects.requireNonNull(row.oid);

        // We're after insert, we can set this for the needs of owned entities (assignments).
        row.objectType = MObjectType.fromSchemaType(schemaObject.getClass());

        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            storeRefs(row, metadata.getCreateApproverRef(),
                    QObjectReferenceMapping.getForCreateApprover(), jdbcSession);
            storeRefs(row, metadata.getModifyApproverRef(),
                    QObjectReferenceMapping.getForModifyApprover(), jdbcSession);
        }

        List<TriggerType> triggers = schemaObject.getTrigger();
        if (!triggers.isEmpty()) {
            triggers.forEach(t -> QTriggerMapping.get().insert(t, row, jdbcSession));
        }

        List<OperationExecutionType> operationExecutions = schemaObject.getOperationExecution();
        if (!operationExecutions.isEmpty()) {
            operationExecutions.forEach(oe ->
                    QOperationExecutionMapping.get().insert(oe, row, jdbcSession));
        }

        storeRefs(row, schemaObject.getParentOrgRef(),
                QObjectReferenceMapping.getForParentOrg(), jdbcSession);
    }

    /**
     * Serializes schema object and sets {@link R#fullObject}.
     */
    public void setFullObject(R row, S schemaObject) throws SchemaException {
        if (schemaObject.getOid() == null || schemaObject.getVersion() == null) {
            throw new IllegalArgumentException(
                    "Serialized object must have assigned OID and version: " + schemaObject);
        }

        row.fullObject = createFullObject(schemaObject);
    }
    // endregion
}
