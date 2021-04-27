/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.*;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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
    public SqlTransformer<S, Q, R> createTransformer(SqlTransformerSupport transformerSupport) {
        return new ObjectSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MObject();
    }
}
