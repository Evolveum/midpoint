/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QAssignmentReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see various static `get*()` methods below.
 * Both mapping instances are initialized (`init*()` methods) in {@link QAssignmentMapping}.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 *
 * @param <AOR> type of the row (M-bean) of the owner (assignment)
 */
public class QAssignmentReferenceMapping<AOR extends MObject>
        extends QReferenceMapping<QAssignmentReference, MAssignmentReference, QAssignment<AOR>, MAssignment> {

    private static QAssignmentReferenceMapping<?> instanceAssignmentCreateApprover;
    private static QAssignmentReferenceMapping<?> instanceAssignmentModifyApprover;

    public static <OR extends MObject> QAssignmentReferenceMapping<OR>
    initForAssignmentCreateApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignmentCreateApprover, repositoryContext)) {
            instanceAssignmentCreateApprover = new QAssignmentReferenceMapping<>(
                    "m_assignment_ref_create_approver", "arefca", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForAssignmentCreateApprover();
    }

    public static <OR extends MObject> QAssignmentReferenceMapping<OR>
    getForAssignmentCreateApprover() {
        //noinspection unchecked
        return (QAssignmentReferenceMapping<OR>)
                Objects.requireNonNull(instanceAssignmentCreateApprover);
    }

    public static <OR extends MObject> QAssignmentReferenceMapping<OR>
    initForAssignmentModifyApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignmentModifyApprover, repositoryContext)) {
            instanceAssignmentModifyApprover = new QAssignmentReferenceMapping<>(
                    "m_assignment_ref_modify_approver", "arefma", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForAssignmentModifyApprover();
    }

    public static <OR extends MObject> QAssignmentReferenceMapping<OR>
    getForAssignmentModifyApprover() {
        //noinspection unchecked
        return (QAssignmentReferenceMapping<OR>)
                Objects.requireNonNull(instanceAssignmentModifyApprover);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QAssignmentReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, QAssignmentReference.class,
                repositoryContext, targetMappingSupplier);

        // assignmentCid probably can't be mapped directly
    }

    @Override
    protected QAssignmentReference newAliasInstance(String alias) {
        return new QAssignmentReference(alias, tableName());
    }

    @Override
    public MAssignmentReference newRowObject(MAssignment ownerRow) {
        MAssignmentReference row = new MAssignmentReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.ownerType;
        row.assignmentCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAssignment<AOR>, QAssignmentReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.assignmentCid));
    }
}
