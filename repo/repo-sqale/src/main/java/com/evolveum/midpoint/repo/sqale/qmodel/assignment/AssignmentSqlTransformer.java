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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AssignmentSqlTransformer
        extends ContainerSqlTransformer<AssignmentType, QAssignment, MAssignment> {

    public AssignmentSqlTransformer(
            SqlTransformerSupport transformerSupport, QAssignmentMapping mapping) {
        super(transformerSupport, mapping);
    }

    // about duplication see the comment in ObjectSqlTransformer.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    public MAssignment toRowObject(
            AssignmentType assignment, MObject ownerRow, JdbcSession jdbcSession) {
        MAssignment row = super.toRowObject(assignment, ownerRow.oid);

        row.ownerType = ownerRow.objectType;
        row.lifecycleState = assignment.getLifecycleState();
        row.orderValue = assignment.getOrder();
        ObjectReferenceType orgRef = assignment.getOrgRef();
        if (orgRef != null) {
            row.orgRefTargetOid = oidToUUid(orgRef.getOid());
            row.orgRefTargetType = schemaTypeToObjectType(orgRef.getType());
            row.orgRefRelationId = processCacheableRelation(orgRef.getRelation(), jdbcSession);
        }
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef != null) {
            row.targetRefTargetOid = oidToUUid(targetRef.getOid());
            row.targetRefTargetType = schemaTypeToObjectType(targetRef.getType());
            row.targetRefRelationId = processCacheableRelation(targetRef.getRelation(), jdbcSession);
        }
        ObjectReferenceType tenantRef = assignment.getTenantRef();
        if (tenantRef != null) {
            row.tenantRefTargetOid = oidToUUid(tenantRef.getOid());
            row.tenantRefTargetType = schemaTypeToObjectType(tenantRef.getType());
            row.tenantRefRelationId = processCacheableRelation(tenantRef.getRelation(), jdbcSession);
        }

        // TODO no idea how to do this yet, somehow related to RAssignment.extension
//        row.extId = assignment.getExtension()...id?;
//        row.extOid =;

        ConstructionType construction = assignment.getConstruction();
        if (construction != null) {
            ObjectReferenceType resourceRef = construction.getResourceRef();
            if (resourceRef != null) {
                row.resourceRefTargetOid = oidToUUid(resourceRef.getOid());
                row.resourceRefTargetType = schemaTypeToObjectType(resourceRef.getType());
                row.resourceRefRelationId = processCacheableRelation(resourceRef.getRelation(), jdbcSession);
            }
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
            ObjectReferenceType creatorRef = metadata.getCreatorRef();
            if (creatorRef != null) {
                row.creatorRefTargetOid = oidToUUid(creatorRef.getOid());
                row.creatorRefTargetType = schemaTypeToObjectType(creatorRef.getType());
                row.creatorRefRelationId = processCacheableRelation(creatorRef.getRelation(), jdbcSession);
            }
            row.createChannelId = processCacheableUri(metadata.getCreateChannel(), jdbcSession);
            row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

            ObjectReferenceType modifierRef = metadata.getModifierRef();
            if (modifierRef != null) {
                row.modifierRefTargetOid = oidToUUid(modifierRef.getOid());
                row.modifierRefTargetType = schemaTypeToObjectType(modifierRef.getType());
                row.modifierRefRelationId = processCacheableRelation(modifierRef.getRelation(), jdbcSession);
            }
            row.modifyChannelId = processCacheableUri(metadata.getModifyChannel(), jdbcSession);
            row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
        }

        // TODO extensions stored inline (JSON)

        return row;
    }
}
