/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 */
public class QAssignmentReferenceMapping
        extends QReferenceMapping<QAssignmentReference, MAssignmentReference> {

    public static final QAssignmentReferenceMapping INSTANCE_ASSIGNMENT_CREATE_APPROVER =
            new QAssignmentReferenceMapping("m_assignment_ref_create_approver", "arefca");
    public static final QAssignmentReferenceMapping INSTANCE_ASSIGNMENT_MODIFY_APPROVER =
            new QAssignmentReferenceMapping("m_assignment_ref_create_approver", "arefma");

    private QAssignmentReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName, QAssignmentReference.class);

        // assignmentCid probably can't be mapped directly
    }

    @Override
    protected QAssignmentReference newAliasInstance(String alias) {
        return new QAssignmentReference(alias);
    }

    @Override
    public MAssignmentReference newRowObject() {
        return new MAssignmentReference();
    }
}
