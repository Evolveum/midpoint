/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.MAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.schema.policy.PolicyRuleIndexData;
import com.evolveum.midpoint.schema.policy.PolicyRuleIndexExtractor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class QPolicyRuleMapping<OR extends MObject>
        extends QContainerMapping<PolicyRuleType, QPolicyRule<OR>, MPolicyRule, OR> {

    public static final String DEFAULT_ALIAS_NAME = "pr";
    private static QPolicyRuleMapping<?> instance;

    // private final MContainerType containerType;
    //private final ItemPath path;

    public static <OR extends MObject> QPolicyRuleMapping<OR> init(@NotNull SqaleRepoContext ctx) {
        if (needsInitialization(instance, ctx)) {
            instance = new QPolicyRuleMapping<>(ctx);
        }
        return get();
    }

    public static <OR extends MObject> QPolicyRuleMapping<OR> get() {
        //noinspection unchecked
        return (QPolicyRuleMapping<OR>) Objects.requireNonNull(instance);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QPolicyRuleMapping(@NotNull SqaleRepoContext ctx) {
        super(QPolicyRule.TABLE_NAME, DEFAULT_ALIAS_NAME,
                PolicyRuleType.class, (Class) QPolicyRule.class, ctx);
        addRelationResolver(PrismConstants.T_PARENT,
                TableRelationResolver.usingJoin(
                        QAssignmentHolderMapping::getAssignmentHolderMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        QPolicyRuleReferenceMapping.initForPolicyRule(ctx);
    }

    @Override
    protected QPolicyRule<OR> newAliasInstance(String alias) {
        return new QPolicyRule<>(alias);
    }

    @Override
    public MPolicyRule newRowObject() {
        return new MPolicyRule();
    }

    @Override
    public MPolicyRule newRowObject(OR ownerRow) {
        MPolicyRule row = newRowObject();
        row.ownerOid = ownerRow.oid;
        row.ownerType = ownerRow.objectType;
        return row;
    }

    @Override
    public MPolicyRule insert(PolicyRuleType rule, OR ownerRow, JdbcSession jdbcSession)
            throws SchemaException {
        MPolicyRule row = initRowObject(rule, ownerRow);
        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);
        row.policyRuleConstraints =
                PolicyRuleIndexSupport.uriIdsForStore(repositoryContext(), data.constraints());
        row.policyRuleActions =
                PolicyRuleIndexSupport.uriIdsForStore(repositoryContext(), data.actions());
        insert(row, jdbcSession);
        QPolicyRuleReferenceMapping.get().insertAll(data.refs(), row, jdbcSession);
        return row;
    }

    @Override
    public void afterModify(SqaleUpdateContext<PolicyRuleType, QPolicyRule<OR>, MPolicyRule> ctx)
            throws SchemaException {
        super.afterModify(ctx);

        PrismContainer<PolicyRuleType> container = ctx.findValueOrItem(MarkType.F_POLICY_RULE);
        if (container == null) {
            return;
        }
        MPolicyRule row = ctx.row();

        PrismContainerValue<PolicyRuleType> pcv = container.findValue(row.cid);
        if (pcv == null) {
            return; // the assignment was deleted, its references go with it (cascade)
        }

        QPolicyRule<OR> pr = ctx.entityPath();
        QPolicyRuleReferenceMapping<OR> refMapping = QPolicyRuleReferenceMapping.get();

        refMapping.deleteAll(row, ctx.jdbcSession());

        PolicyRuleType policyRule = pcv.asContainerable();

        PolicyRuleIndexData index = PolicyRuleIndexExtractor.extract(policyRule);
        ctx.set(pr.policyRuleConstraints,
                PolicyRuleIndexSupport.uriIdsForStore(repositoryContext(), index.constraints()));
        ctx.set(pr.policyRuleActions,
                PolicyRuleIndexSupport.uriIdsForStore(repositoryContext(), index.actions()));
        refMapping.insertAll(index.refs(), row, ctx.jdbcSession());
    }
}


