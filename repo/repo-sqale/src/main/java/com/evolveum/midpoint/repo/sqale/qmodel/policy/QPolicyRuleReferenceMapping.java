/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
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

public class QPolicyRuleReferenceMapping<AOR extends MObject> extends
        QReferenceMapping<QPolicyRuleReference, MPolicyRuleReference,
                QPolicyRule<AOR>, MPolicyRule> {

    public static final String DEFAULT_ALIAS_NAME = "prrefmap";

    private static QPolicyRuleReferenceMapping<?> instance;

    public static <AOR extends MObject> QPolicyRuleReferenceMapping<AOR> get() {
        //noinspection unchecked
        return (QPolicyRuleReferenceMapping<AOR>) Objects.requireNonNull(instance);
    }

    @Override
    protected QPolicyRuleReference newAliasInstance(String alias) {
        return new QPolicyRuleReference(alias);
    }

//    public QPolicyRuleReferenceMapping( @NotNull SqaleRepoContext repositoryContext, Object getObjectMapping) {
//        super();
//    }

    public static <OR extends MObject> QPolicyRuleReferenceMapping<OR> initForPolicyRule(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QPolicyRuleReferenceMapping<>(
                    repositoryContext, QObjectMapping::getObjectMapping);
        }
        return get();
    }

    private <TQ extends QObject<TR>, TR extends MObject> QPolicyRuleReferenceMapping(
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(QAssignmentPolicyRuleReference.TABLE_NAME, DEFAULT_ALIAS_NAME,
                QPolicyRuleReference.class, repositoryContext, targetMappingSupplier);
    }

    /** Stores the references of the rule of the assignment; the assignment row must already exist. */
    public void insertAll(
            @NotNull Collection<IndexedRef> refs, @NotNull MPolicyRule ownerRow, @NotNull JdbcSession jdbcSession) {
        // Relations are normalized only here, so the final check for duplicates must be here too.
        Set<List<Object>> storedKeys = new HashSet<>();
        for (IndexedRef ref : refs) {
            MPolicyRuleReference row = newRowObject(ownerRow);
            if (!PolicyRuleIndexSupport.fillTarget(row, ref, repositoryContext())) {
                continue;
            }
            row.policyReferenceType = MPolicyReferenceType.from(ref.holder(), ref.refItem());
            if (storedKeys.add(List.of(row.policyReferenceType, row.relationId, row.targetOid))) {
                insert(row, jdbcSession);
            }
        }
    }

    @Override
    public MPolicyRuleReference newRowObject(MPolicyRule ownerRow) {
        MPolicyRuleReference row = new MPolicyRuleReference();

        row.policyRuleCid = ownerRow.cid;
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.ownerType;

        return row;

    }

    public void deleteAll(MPolicyRule ownerRow, JdbcSession jdbcSession) {
        QPolicyRuleReference r = defaultAlias();
        jdbcSession.newDelete(r)
                .where(r.isOwnedBy(ownerRow))
                .execute();
    }

    @Override
    public BiFunction<QPolicyRule<AOR>, QPolicyRuleReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.policyRuleCid));
    }

}
