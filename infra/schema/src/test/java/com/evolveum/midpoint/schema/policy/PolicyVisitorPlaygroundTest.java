/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyVisitorPlaygroundTest extends AbstractSchemaTest {

    private static final Trace log = TraceManager.getTrace(PolicyVisitorPlaygroundTest.class);

    private PolicyConstraintsType testPolicyConstraintType() {
        PolicyConstraintsType pct = new PolicyConstraintsType();
        PolicyConstraintsType pct2 = new PolicyConstraintsType();
        pct.exclusion(new ExclusionPolicyConstraintType().targetRef("A", RoleType.COMPLEX_TYPE));
        return pct;
    }

    private PolicyRuleType testPolicyRuleType() {
        var rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef("A", RoleType.COMPLEX_TYPE))
                        .and(new PolicyConstraintsType()
                                .modification(new ModificationPolicyConstraintType())
                                .not(new PolicyConstraintsType()
                                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                                .targetRef("B", RoleType.COMPLEX_TYPE)))));

        var rule2 = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef("A", RoleType.COMPLEX_TYPE))
                        .exclusion(new ExclusionPolicyConstraintType()          // duplicita: rovnaký cieľ
                                .targetRef("A", RoleType.COMPLEX_TYPE))
                        .and(new PolicyConstraintsType()
                                .modification(new ModificationPolicyConstraintType())
                                .not(new PolicyConstraintsType()
                                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                                .targetRef("B", RoleType.COMPLEX_TYPE)))));

        return rule2;

    }

    @Test
    public void printVisitor() {
        Set<String> constraints = new HashSet<>();
        Set<String> refs = new HashSet<>();

        PolicyRuleTypeUtil.accept(testPolicyRuleType().getPolicyConstraints(), (name, c) -> {
            log.info(name.getNamespaceURI() + "  ->  " + c.getClass().getSimpleName());
            //   if (!name.getLocalPart().equalsIgnoreCase("and"))
            {constraints.add(name.getLocalPart());}
            if (c instanceof ExclusionPolicyConstraintType ex && ex.getTargetRef() != null) {
                log.info("   ref " + name.getLocalPart() + "/targetRef -> " + ex.getTargetRef().getOid());
                // constraints.add(name.getLocalPart());
            }
            if (c instanceof HasAssignmentPolicyConstraintType ha && ha.getTargetRef() != null) {
                log.info("   ref " + name.getLocalPart() + "/targetRef -> " + ha.getTargetRef().getOid());
                //    constraints.add(name.getLocalPart());
            }
            return true;
        }, true, false, null, true);
        assertThat(constraints).containsExactlyInAnyOrder("exclusion", "and", "modification", "not", "hasAssignment");
    }

    private static final Set<QName> COMPOSITES = Set.of(
            PolicyConstraintsType.F_AND, PolicyConstraintsType.F_OR, PolicyConstraintsType.F_NOT);

    @Test
    public void step2PrintRefs() {
        Set<QName> constraints = new LinkedHashSet<>();
        Set<IndexedRef> refs = new LinkedHashSet<>();

        PolicyRuleTypeUtil.accept(this.testPolicyRuleType().getPolicyConstraints(), (name, c) -> {
            if (!COMPOSITES.contains(name)) {
                constraints.add(name);
            }
            if (c instanceof ExclusionPolicyConstraintType ex) {               // exclusion, requirement
                addRef(refs, name, ExclusionPolicyConstraintType.F_TARGET_REF, ex.getTargetRef());
                addRef(refs, name, ExclusionPolicyConstraintType.F_TARGET_ARCHETYPE_REF, ex.getTargetArchetypeRef());
            } else if (c instanceof HasAssignmentPolicyConstraintType ha) {    // hasAssignment, hasNoAssignment
                addRef(refs, name, HasAssignmentPolicyConstraintType.F_TARGET_REF, ha.getTargetRef());
                addRef(refs, name, HasAssignmentPolicyConstraintType.F_TARGET_ARCHETYPE_REF, ha.getTargetArchetypeRef());
            }
            return true;
        }, true, false, null, true);

        log.info("constraints:");
        constraints.forEach(c -> log.info("  " + c.getLocalPart()));

        log.info("refs:");
        refs.forEach(r -> log.info("  " + r.path() + " -> " + r.targetOid()
                + " (" + localPart(r.targetType()) + ", relation " + localPart(r.relation()) + ")"));
    }

    private static void addRef(Set<IndexedRef> refs, QName constraintName, QName refItem, ObjectReferenceType ref) {
        IndexedRef indexed = IndexedRef.of(PolicyRuleIndexPaths.of(constraintName, refItem), ref);
        if (indexed != null) {
            refs.add(indexed);
        }
    }

    private static String localPart(QName q) {
        return q != null ? q.getLocalPart() : "-";
    }

}
