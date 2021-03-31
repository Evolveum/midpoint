/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor.uuidMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ARCHETYPE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ROLE_MEMBERSHIP_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.*;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.TableRelationResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
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

        addItemMapping(PrismConstants.T_ID, uuidMapper(path(q -> q.oid)));
        addItemMapping(F_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.nameOrig), path(q -> q.nameNorm)));
        addItemMapping(F_TENANT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.tenantRefTargetOid),
                path(q -> q.tenantRefTargetType),
                path(q -> q.tenantRefRelationId)));
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(path(q -> q.lifecycleState)));
        // version/cid_seq is not mapped for queries or deltas, it's managed by repo explicitly

        // TODO mapper for policySituations and subtypes
        // TODO ext mapping can't be done statically

        addNestedMapping(F_METADATA, MetadataType.class)
                .addItemMapping(MetadataType.F_CREATOR_REF, RefItemFilterProcessor.mapper(
                        path(q -> q.creatorRefTargetOid),
                        path(q -> q.creatorRefTargetType),
                        path(q -> q.creatorRefRelationId)))
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        UriItemFilterProcessor.mapper(path(q -> q.createChannelId)))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.createTimestamp)))
                .addItemMapping(MetadataType.F_MODIFIER_REF, RefItemFilterProcessor.mapper(
                        path(q -> q.modifierRefTargetOid),
                        path(q -> q.modifierRefTargetType),
                        path(q -> q.modifierRefRelationId)))
                .addItemMapping(MetadataType.F_MODIFY_CHANNEL,
                        UriItemFilterProcessor.mapper(path(q -> q.modifyChannelId)))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.modifyTimestamp)))
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        QObjectReferenceMapping.INSTANCE_OBJECT_CREATE_APPROVER)
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        QObjectReferenceMapping.INSTANCE_OBJECT_MODIFY_APPROVER);

        // AssignmentHolderType
        addRefMapping(F_ARCHETYPE_REF, QObjectReferenceMapping.INSTANCE_ARCHETYPE);
        addRefMapping(F_PARENT_ORG_REF, QObjectReferenceMapping.INSTANCE_OBJECT_PARENT_ORG);
        addRefMapping(F_ROLE_MEMBERSHIP_REF, QObjectReferenceMapping.INSTANCE_ROLE_MEMBERSHIP);

        addRelationResolver(AssignmentHolderType.F_ASSIGNMENT,
                new TableRelationResolver<>(QAssignment.class,
                        joinOn((o, a) -> o.oid.eq(a.ownerOid))));
        addRelationResolver(F_TRIGGER,
                new TableRelationResolver<>(QTrigger.class,
                        joinOn((o, t) -> o.oid.eq(t.ownerOid))));
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
