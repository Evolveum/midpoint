/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ContainerSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

public class AssignmentSqlTransformer
        extends ContainerSqlTransformer<AssignmentType, QAssignment, MAssignment> {

    public AssignmentSqlTransformer(
            SqlTransformerSupport transformerSupport, QAssignmentMapping mapping) {
        super(transformerSupport, mapping);
    }

    // about duplication see the comment in ObjectSqlTransformer.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    public MAssignment insert(
            AssignmentType assignment, MObject ownerRow, JdbcSession jdbcSession) {
        MAssignment row = initRowObject(assignment, ownerRow.oid);

        row.ownerType = ownerRow.objectType;
        row.lifecycleState = assignment.getLifecycleState();
        row.orderValue = assignment.getOrder();
        setReference(assignment.getOrgRef(), jdbcSession,
                o -> row.orgRefTargetOid = o,
                t -> row.orgRefTargetType = t,
                r -> row.orgRefRelationId = r);
        setReference(assignment.getTargetRef(), jdbcSession,
                o -> row.targetRefTargetOid = o,
                t -> row.targetRefTargetType = t,
                r -> row.targetRefRelationId = r);
        setReference(assignment.getTenantRef(), jdbcSession,
                o -> row.tenantRefTargetOid = o,
                t -> row.tenantRefTargetType = t,
                r -> row.tenantRefRelationId = r);

        // TODO no idea how to do this yet, somehow related to RAssignment.extension
//        row.extId = assignment.getExtension()...id?;
//        row.extOid =;

        ConstructionType construction = assignment.getConstruction();
        if (construction != null) {
            setReference(construction.getResourceRef(), jdbcSession,
                    o -> row.resourceRefTargetOid = o,
                    t -> row.resourceRefTargetType = t,
                    r -> row.resourceRefRelationId = r);
        }

        ActivationType activation = assignment.getActivation();
        if (activation != null) {
            row.administrativeStatus = activation.getAdministrativeStatus();
            row.effectiveStatus = activation.getEffectiveStatus();
            row.enableTimestamp = MiscUtil.asInstant(activation.getEnableTimestamp());
            row.disableTimestamp = MiscUtil.asInstant(activation.getDisableTimestamp());
            row.disableReason = activation.getDisableReason();
            row.validityStatus = activation.getValidityStatus();
            row.validFrom = MiscUtil.asInstant(activation.getValidFrom());
            row.validTo = MiscUtil.asInstant(activation.getValidTo());
            row.validityChangeTimestamp = MiscUtil.asInstant(activation.getValidityChangeTimestamp());
            row.archiveTimestamp = MiscUtil.asInstant(activation.getArchiveTimestamp());
        }

        MetadataType metadata = assignment.getMetadata();
        if (metadata != null) {
            setReference(metadata.getCreatorRef(), jdbcSession,
                    o -> row.creatorRefTargetOid = o,
                    t -> row.creatorRefTargetType = t,
                    r -> row.creatorRefRelationId = r);
            row.createChannelId = processCacheableUri(metadata.getCreateChannel(), jdbcSession);
            row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

            setReference(metadata.getModifierRef(), jdbcSession,
                    o -> row.modifierRefTargetOid = o,
                    t -> row.modifierRefTargetType = t,
                    r -> row.modifierRefRelationId = r);
            row.modifyChannelId = processCacheableUri(metadata.getModifyChannel(), jdbcSession);
            row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
        }

        // TODO extensions stored inline (JSON)

        // insert before treating sub-entities
        insert(row, jdbcSession);

        if (metadata != null) {
            storeRefs(row, metadata.getCreateApproverRef(),
                    QAssignmentReferenceMapping.INSTANCE_ASSIGNMENT_CREATE_APPROVER, jdbcSession);
            storeRefs(row, metadata.getModifyApproverRef(),
                    QAssignmentReferenceMapping.INSTANCE_ASSIGNMENT_MODIFY_APPROVER, jdbcSession);
        }

        return row;
    }
}
