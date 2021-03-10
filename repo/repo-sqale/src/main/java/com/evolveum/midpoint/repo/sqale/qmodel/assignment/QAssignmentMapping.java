/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.integerMapper;
import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * Mapping between {@link QAssignment} and {@link AssignmentType}.
 */
public class QAssignmentMapping
        extends QContainerMapping<AssignmentType, QAssignment, MAssignment> {

    public static final String DEFAULT_ALIAS_NAME = "a";

    public static final QAssignmentMapping INSTANCE = new QAssignmentMapping();

    private QAssignmentMapping() {
        super(QAssignment.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentType.class, QAssignment.class);

        // TODO OWNER_TYPE is new thing and can help avoid join to concrete object table
        //  But this will likely require special treatment/heuristic.
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(path(q -> q.lifecycleState)));
        addItemMapping(F_ORDER, integerMapper(path(q -> q.orderValue)));
        addItemMapping(F_ORG_REF, RefItemFilterProcessor.mapper(
                path(q -> q.orgRefTargetOid),
                path(q -> q.orgRefTargetType),
                path(q -> q.orgRefRelationId)));
        addItemMapping(F_TARGET_REF, RefItemFilterProcessor.mapper(
                path(q -> q.targetRefTargetOid),
                path(q -> q.targetRefTargetType),
                path(q -> q.targetRefRelationId)));
        addItemMapping(F_TENANT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.tenantRefTargetOid),
                path(q -> q.tenantRefTargetType),
                path(q -> q.tenantRefRelationId)));
        // TODO no idea how extId/Oid works, see RAssignment.getExtension
        // TODO ext mapping can't be done statically
        nestedMapping(F_CONSTRUCTION, ConstructionType.class)
                .addItemMapping(ConstructionType.F_RESOURCE_REF, RefItemFilterProcessor.mapper(
                        path(q -> q.resourceRefTargetOid),
                        path(q -> q.resourceRefTargetType),
                        path(q -> q.resourceRefRelationId)));
        nestedMapping(F_ACTIVATION, ActivationType.class)
                .addItemMapping(ActivationType.F_ADMINISTRATIVE_STATUS,
                        integerMapper(path(q -> q.administrativeStatus)))
                .addItemMapping(ActivationType.F_EFFECTIVE_STATUS,
                        integerMapper(path(q -> q.effectiveStatus)))
                .addItemMapping(ActivationType.F_ENABLE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.enableTimestamp)))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        TimestampItemFilterProcessor.mapper(path(q -> q.disableTimestamp)))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        stringMapper(path(q -> q.disableReason)))
                .addItemMapping(ActivationType.F_VALIDITY_STATUS,
                        integerMapper(path(q -> q.validityStatus)))
                .addItemMapping(ActivationType.F_VALID_FROM,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validFrom)))
                .addItemMapping(ActivationType.F_VALID_TO,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validTo)))
                .addItemMapping(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validityChangeTimestamp)))
                .addItemMapping(ActivationType.F_ARCHIVE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.archiveTimestamp)));
        nestedMapping(F_METADATA, MetadataType.class)
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
                        TimestampItemFilterProcessor.mapper(path(q -> q.modifyTimestamp)));

        // TODO relation mapping (often in nested mapping ;-))
    }

    @Override
    protected QAssignment newAliasInstance(String alias) {
        return new QAssignment(alias);
    }

    @Override
    public AssignmentSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new AssignmentSqlTransformer(transformerSupport, this);
    }

    @Override
    public MAssignment newRowObject() {
        return new MAssignment();
    }
}
