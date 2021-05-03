/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.*;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.repo.sqale.SqaleSupportService;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlSupportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    public static final QObjectMapping<ObjectType, QObject<MObject>, MObject> INSTANCE =
            new QObjectMapping<>(QObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    ObjectType.class, QObject.CLASS);

    protected QObjectMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        addItemMapping(PrismConstants.T_ID, uuidMapper(q -> q.oid));
        addItemMapping(F_NAME, polyStringMapper(
                q -> q.nameOrig, q -> q.nameNorm));
        addItemMapping(F_TENANT_REF, refMapper(
                q -> q.tenantRefTargetOid,
                q -> q.tenantRefTargetType,
                q -> q.tenantRefRelationId));
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(q -> q.lifecycleState));
        // version/cid_seq is not mapped for queries or deltas, it's managed by repo explicitly

        // TODO mapper for policySituations and subtypes
        // TODO ext mapping can't be done statically

        addNestedMapping(F_METADATA, MetadataType.class)
                .addItemMapping(MetadataType.F_CREATOR_REF, refMapper(
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId))
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addItemMapping(MetadataType.F_MODIFIER_REF, refMapper(
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId))
                .addItemMapping(MetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp))
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        objectCreateApproverReferenceMapping())
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        objectModifyApproverReferenceMapping());

        addRefMapping(F_PARENT_ORG_REF, objectParentOrgReferenceMapping());

        addContainerTableMapping(AssignmentHolderType.F_ASSIGNMENT, assignmentMapping(),
                joinOn((o, a) -> o.oid.eq(a.ownerOid)));
        addContainerTableMapping(F_OPERATION_EXECUTION, operationExecutionMapping(),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid)));
        addContainerTableMapping(F_TRIGGER, triggerMapping(),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid)));

        // AssignmentHolderType
        addRefMapping(F_ARCHETYPE_REF, archetypeReferenceMapping());
        addRefMapping(F_DELEGATED_REF, delegatedReferenceMapping());
        addRefMapping(F_ROLE_MEMBERSHIP_REF, roleMembershipReferenceMapping());
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    @NotNull
    public QAssignmentMapping<R> assignmentMapping() {
        //noinspection unchecked
        return (QAssignmentMapping<R>) QAssignmentMapping.INSTANCE;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    @NotNull
    public QOperationExecutionMapping<R> operationExecutionMapping() {
        //noinspection unchecked
        return (QOperationExecutionMapping<R>) QOperationExecutionMapping.INSTANCE;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    @NotNull
    public QTriggerMapping<R> triggerMapping() {
        //noinspection unchecked
        return (QTriggerMapping<R>) QTriggerMapping.INSTANCE;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> archetypeReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) QObjectReferenceMapping.INSTANCE_ARCHETYPE;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> delegatedReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) QObjectReferenceMapping.INSTANCE_DELEGATED;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> objectCreateApproverReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>)
                QObjectReferenceMapping.INSTANCE_OBJECT_CREATE_APPROVER;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> objectModifyApproverReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>)
                QObjectReferenceMapping.INSTANCE_OBJECT_MODIFY_APPROVER;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> objectParentOrgReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) QObjectReferenceMapping.INSTANCE_OBJECT_PARENT_ORG;
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    public @NotNull QObjectReferenceMapping<Q, R> roleMembershipReferenceMapping() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) QObjectReferenceMapping.INSTANCE_ROLE_MEMBERSHIP;
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
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

        PrismObject<S> prismObject;
        String serializedForm = new String(fullObject, StandardCharsets.UTF_8);
        try {
            SqlSupportService.ParseResult<S> result =
                    SqaleSupportService.getInstance().parsePrismObject(serializedForm);
            prismObject = result.prismObject;
            if (result.parsingContext.hasWarnings()) {
                logger.warn("Object {} parsed with {} warnings",
                        ObjectTypeUtil.toShortString(prismObject),
                        result.parsingContext.getWarnings().size());
            }
        } catch (SchemaException | RuntimeException | Error e) {
            // This is a serious thing. We have corrupted XML in the repo. This may happen even
            // during system init. We want really loud and detailed error here.
            logger.error("Couldn't parse object {} {}: {}: {}\n{}",
                    schemaType().getSimpleName(), row.get(entityPath.oid),
                    e.getClass().getName(), e.getMessage(), serializedForm, e);
            throw e;
        }

        return prismObject.asObjectable();
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

        row.oid = oidToUUid(schemaObject.getOid());
        // objectType MUST be left NULL for INSERT, it's determined by PG
        setPolyString(schemaObject.getName(), o -> row.nameOrig = o, n -> row.nameNorm = n);
        // fullObject is managed outside of this method
        setReference(schemaObject.getTenantRef(),
                o -> row.tenantRefTargetOid = o,
                t -> row.tenantRefTargetType = t,
                r -> row.tenantRefRelationId = r);
        row.lifecycleState = schemaObject.getLifecycleState();
        // containerIdSeq is managed outside of this method
        row.version = SqaleUtils.objectVersionAsInt(schemaObject);

        // complex DB fields
        row.policySituations = processCacheableUris(schemaObject.getPolicySituation());
        row.subtypes = arrayFor(schemaObject.getSubtype());
        // TODO textInfo (fulltext support)
        //  repo.getTextInfoItems().addAll(RObjectTextInfo.createItemsSet(jaxb, repo, repositoryContext));
        // TODO extensions stored inline (JSON) - that is ext column

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
            @NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) {
        Objects.requireNonNull(row.oid);

        // We're after insert, we can set this for the needs of owned entities (assignments).
        row.objectType = MObjectType.fromSchemaType(schemaObject.getClass());

        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            storeRefs(row, metadata.getCreateApproverRef(),
                    objectCreateApproverReferenceMapping(), jdbcSession);
            storeRefs(row, metadata.getModifyApproverRef(),
                    objectModifyApproverReferenceMapping(), jdbcSession);
        }

        List<TriggerType> triggers = schemaObject.getTrigger();
        if (!triggers.isEmpty()) {
            triggers.forEach(t -> triggerMapping().insert(t, row, jdbcSession));
        }

        List<OperationExecutionType> operationExecutions = schemaObject.getOperationExecution();
        if (!operationExecutions.isEmpty()) {
            operationExecutions.forEach(oe ->
                    operationExecutionMapping().insert(oe, row, jdbcSession));
        }

        storeRefs(row, schemaObject.getParentOrgRef(),
                objectParentOrgReferenceMapping(), jdbcSession);

        if (schemaObject instanceof AssignmentHolderType) {
            storeAssignmentHolderEntities(row, (AssignmentHolderType) schemaObject, jdbcSession);
        }

        /* TODO EAV extensions - the relevant code from old repo RObject#copyObjectInformationFromJAXB
        if (jaxb.getExtension() != null) {
            copyExtensionOrAttributesFromJAXB(jaxb.getExtension().asPrismContainerValue(), repo, repositoryContext, RObjectExtensionType.EXTENSION, generatorResult);
        }
        */
    }

    private void storeAssignmentHolderEntities(
            R row, AssignmentHolderType schemaObject, JdbcSession jdbcSession) {
        List<AssignmentType> assignments = schemaObject.getAssignment();
        if (!assignments.isEmpty()) {
            assignments.forEach(assignment ->
                    assignmentMapping().insert(assignment, row, jdbcSession));
        }

        storeRefs(row, schemaObject.getArchetypeRef(),
                archetypeReferenceMapping(), jdbcSession);
        storeRefs(row, schemaObject.getDelegatedRef(),
                delegatedReferenceMapping(), jdbcSession);
        storeRefs(row, schemaObject.getRoleMembershipRef(),
                roleMembershipReferenceMapping(), jdbcSession);
    }

    /**
     * Serializes schema object and sets {@link R#fullObject}.
     */
    public void setFullObject(R row, S schemaObject) throws SchemaException {
        row.fullObject = createFullObject(schemaObject);
    }

    public byte[] createFullObject(S schemaObject) throws SchemaException {
        if (schemaObject.getOid() == null || schemaObject.getVersion() == null) {
            throw new IllegalArgumentException(
                    "Serialized object must have assigned OID and version: " + schemaObject);
        }

        return SqaleSupportService.getInstance().createStringSerializer()
                .itemsToSkip(fullObjectItemsToSkip())
                .options(SerializationOptions
                        .createSerializeReferenceNamesForNullOids()
                        .skipIndexOnly(true)
                        .skipTransient(true))
                .serialize(schemaObject.asPrismObject())
                .getBytes(StandardCharsets.UTF_8);
    }

    protected Collection<? extends QName> fullObjectItemsToSkip() {
        // TODO extend later, things like FocusType.F_JPEG_PHOTO, see ObjectUpdater#updateFullObject
        return Collections.emptyList();
    }
    // endregion
}
