/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.MAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;

import com.evolveum.midpoint.schema.policy.IndexedRef;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class QAssignmentPolicyRuleReferenceMapping<AOR extends MObject> extends
        QReferenceMapping<QAssignmentPolicyRuleReference, MAssignmentPolicyRuleReference,
        QAssignment<AOR>, MAssignment> {

    public static final String DEFAULT_ALIAS_NAME = "arefpr";

    private static QAssignmentPolicyRuleReferenceMapping<?> instance;

    public static <OR extends MObject> QAssignmentPolicyRuleReferenceMapping<OR> initForAssignmentPolicyRule(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAssignmentPolicyRuleReferenceMapping<>(
                    repositoryContext, QObjectMapping::getObjectMapping);
        }
        return get();
    }

    public static <OR extends MObject> QAssignmentPolicyRuleReferenceMapping<OR> get() {
        //noinspection unchecked
        return (QAssignmentPolicyRuleReferenceMapping<OR>) Objects.requireNonNull(instance);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QAssignmentPolicyRuleReferenceMapping(
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(QAssignmentPolicyRuleReference.TABLE_NAME, DEFAULT_ALIAS_NAME,
                QAssignmentPolicyRuleReference.class, repositoryContext, targetMappingSupplier);
    }
    @Override
    protected QAssignmentPolicyRuleReference newAliasInstance(String alias) {
        return new QAssignmentPolicyRuleReference(alias);
    }

    @Override
    public MAssignmentPolicyRuleReference newRowObject(MAssignment ownerRow) {
        MAssignmentPolicyRuleReference row = new MAssignmentPolicyRuleReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.ownerType;
        row.assignmentCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAssignment<AOR>, QAssignmentPolicyRuleReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.assignmentCid));
    }

    /** Stores the references of the rule of the assignment; the assignment row must already exist. */
    public void insertAll(
            @NotNull Collection<IndexedRef> refs, @NotNull MAssignment ownerRow, @NotNull JdbcSession jdbcSession) {
        // Relations are normalized only here, so the final check for duplicates must be here too.
        Set<List<Object>> storedKeys = new HashSet<>();
        for (IndexedRef ref : refs) {
            MAssignmentPolicyRuleReference row = newRowObject(ownerRow);
            if (!PolicyRuleIndexSupport.fillTarget(row, ref, repositoryContext())) {
                continue;
            }
            row.policyReferenceType = MPolicyReferenceType.from(ref.holder(), ref.refItem());
            if (storedKeys.add(List.of(row.policyReferenceType, row.relationId, row.targetOid))) {
                insert(row, jdbcSession);
            }
        }
    }

}
