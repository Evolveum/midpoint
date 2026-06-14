/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection.NO_DELEGATOR;
import static org.testng.AssertJUnit.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.selector.eval.ClauseProcessingContextDescription;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.OrgTreeEvaluator;
import com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext;
import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgRelationObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests post-filter matching for orgRelation selectors.
 *
 * Regression focus: ALL_DESCENDANTS should batch descendant checks for all matching
 * subject org refs, while preserving includeReferenceOrg and subjectRelation semantics.
 */
public class OrgRelationClauseBatchingTest extends AbstractSchemaTest {

    private static final String ORG_A = "00000000-0000-0000-0000-0000000000a1";
    private static final String ORG_B = "00000000-0000-0000-0000-0000000000b1";
    private static final String ORG_C = "00000000-0000-0000-0000-0000000000c1";
    private static final String TARGET = "00000000-0000-0000-0000-000000000010";
    private static final QName CUSTOM_TARGET_RELATION = QName.valueOf("{https://example.com/relations}custom");

    /**
     * Principal has multiple parentOrgRefs, but only manager refs are selected by subjectRelation.
     * The selected ancestor OIDs should be passed to one batched isDescendantOfAny call, not checked one by one.
     */
    @Test
    public void testAllDescendantsUsesOneBatchedDescendantLookup() throws Exception {
        TrackingOrgTreeEvaluator evaluator = new TrackingOrgTreeEvaluator();
        UserType principal = principal(
                parentOrgRef(ORG_A, SchemaConstants.ORG_MANAGER),
                parentOrgRef(ORG_B, SchemaConstants.ORG_DEFAULT),
                parentOrgRef(ORG_C, SchemaConstants.ORG_MANAGER));
        UserType target = targetUser(TARGET, parentOrgRef("00000000-0000-0000-0000-000000000111", CUSTOM_TARGET_RELATION));

        boolean matches = clause(SchemaConstants.ORG_MANAGER, false)
                .matches(target.asPrismObject().getValue(), context(principal, evaluator));

        assertTrue(matches);
        assertEquals(1, evaluator.isDescendantOfAnyCalls);
        assertEquals(0, evaluator.isDescendantCalls);
        assertEquals(Set.of(ORG_A, ORG_C), evaluator.lastAncestorOrgOids);
    }

    /**
     * When the target object is one of the matching subject orgs, includeReferenceOrg=true is sufficient to match.
     * This self branch is separate from descendant membership and should avoid any org-closure lookup.
     */
    @Test
    public void testIncludeReferenceOrgSelfMatchAvoidsDescendantLookup() throws Exception {
        TrackingOrgTreeEvaluator evaluator = new TrackingOrgTreeEvaluator();
        UserType principal = principal(parentOrgRef(ORG_A, SchemaConstants.ORG_MANAGER));
        OrgType target = new OrgType().oid(ORG_A).name("org-a");

        boolean matches = clause(SchemaConstants.ORG_MANAGER, true)
                .matches(target.asPrismObject().getValue(), context(principal, evaluator));

        assertTrue(matches);
        assertEquals(0, evaluator.isDescendantOfAnyCalls);
        assertEquals(0, evaluator.isDescendantCalls);
    }

    /**
     * The same target/self setup must not match just because the OIDs are equal when includeReferenceOrg=false.
     * In that case the clause should continue to descendant evaluation and return what the evaluator returns.
     */
    @Test
    public void testIncludeReferenceOrgFalseDoesNotSelfMatch() throws Exception {
        TrackingOrgTreeEvaluator evaluator = new TrackingOrgTreeEvaluator();
        evaluator.isDescendantOfAnyReturnValue = false;
        UserType principal = principal(parentOrgRef(ORG_A, SchemaConstants.ORG_MANAGER));
        OrgType target = new OrgType().oid(ORG_A).name("org-a");

        boolean matches = clause(SchemaConstants.ORG_MANAGER, false)
                .matches(target.asPrismObject().getValue(), context(principal, evaluator));

        assertFalse(matches);
        assertEquals(1, evaluator.isDescendantOfAnyCalls);
        assertEquals(Set.of(ORG_A), evaluator.lastAncestorOrgOids);
    }

    /**
     * subjectRelation=manager selects manager refs from the principal only.
     * The target object is passed to descendant evaluation with its original non-manager parentOrgRef relation intact,
     * proving that target-side relation is not accidentally constrained by subjectRelation.
     */
    @Test
    public void testSubjectRelationFiltersPrincipalRefsOnly() throws Exception {
        TrackingOrgTreeEvaluator evaluator = new TrackingOrgTreeEvaluator();
        UserType principal = principal(
                parentOrgRef(ORG_A, SchemaConstants.ORG_MANAGER),
                parentOrgRef(ORG_B, SchemaConstants.ORG_DEFAULT));
        UserType target = targetUser(
                TARGET,
                parentOrgRef("00000000-0000-0000-0000-000000000222", CUSTOM_TARGET_RELATION));

        boolean matches = clause(SchemaConstants.ORG_MANAGER, false)
                .matches(target.asPrismObject().getValue(), context(principal, evaluator));

        assertTrue(matches);
        assertEquals(Set.of(ORG_A), evaluator.lastAncestorOrgOids);
        assertEquals(TARGET, evaluator.lastObject.getOid());
        assertEquals(
                CUSTOM_TARGET_RELATION,
                ((UserType) evaluator.lastObject.asObjectable()).getParentOrgRef().get(0).getRelation());
    }

    /**
     * Principal has parentOrgRefs, but none match subjectRelation=manager.
     * With no selected ancestor orgs there is nothing to batch, so the clause should return false immediately.
     */
    @Test
    public void testNoMatchingSubjectRelationsReturnsFalseWithoutLookup() throws Exception {
        TrackingOrgTreeEvaluator evaluator = new TrackingOrgTreeEvaluator();
        UserType principal = principal(parentOrgRef(ORG_B, SchemaConstants.ORG_DEFAULT));
        UserType target = targetUser(TARGET);

        boolean matches = clause(SchemaConstants.ORG_MANAGER, false)
                .matches(target.asPrismObject().getValue(), context(principal, evaluator));

        assertFalse(matches);
        assertEquals(0, evaluator.isDescendantOfAnyCalls);
        assertEquals(0, evaluator.isDescendantCalls);
    }

    private OrgRelationClause clause(QName subjectRelation, boolean includeReferenceOrg) {
        OrgRelationObjectSpecificationType bean = new OrgRelationObjectSpecificationType();
        bean.setScope(OrgScopeType.ALL_DESCENDANTS);
        bean.setSubjectRelation(subjectRelation);
        bean.setIncludeReferenceOrg(includeReferenceOrg);
        return OrgRelationClause.of(bean);
    }

    private MatchingContext context(FocusType principal, OrgTreeEvaluator evaluator) {
        return new MatchingContext(
                null,
                new NoOpTracer(),
                evaluator,
                new TestSubjectedEvaluationContext(principal),
                null,
                null,
                ClauseProcessingContextDescription.defaultOne(),
                NO_DELEGATOR,
                true);
    }

    private UserType principal(ObjectReferenceType... parentOrgRefs) {
        UserType principal = new UserType().oid("00000000-0000-0000-0000-000000000001").name("principal");
        for (ObjectReferenceType parentOrgRef : parentOrgRefs) {
            principal.getParentOrgRef().add(parentOrgRef);
        }
        return principal;
    }

    private UserType targetUser(String oid, ObjectReferenceType... parentOrgRefs) {
        UserType target = new UserType().oid(oid).name("target");
        for (ObjectReferenceType parentOrgRef : parentOrgRefs) {
            target.getParentOrgRef().add(parentOrgRef);
        }
        return target;
    }

    private ObjectReferenceType parentOrgRef(String oid, QName relation) {
        return new ObjectReferenceType()
                .oid(oid)
                .type(OrgType.COMPLEX_TYPE)
                .relation(relation);
    }

    private static class TrackingOrgTreeEvaluator implements OrgTreeEvaluator {

        private boolean isDescendantOfAnyReturnValue = true;
        private int isDescendantCalls;
        private int isDescendantOfAnyCalls;
        private PrismObject<?> lastObject;
        private Set<String> lastAncestorOrgOids;

        @Override
        public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String ancestorOrgOid) {
            isDescendantCalls++;
            return false;
        }

        @Override
        public <O extends ObjectType> boolean isDescendantOfAny(
                PrismObject<O> object, Collection<String> ancestorOrgOids) {
            isDescendantOfAnyCalls++;
            lastObject = object;
            lastAncestorOrgOids = new LinkedHashSet<>(ancestorOrgOids);
            return isDescendantOfAnyReturnValue;
        }

        @Override
        public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String descendantOrgOid) {
            return false;
        }
    }

    private record TestSubjectedEvaluationContext(FocusType principal) implements SubjectedEvaluationContext {

        @Override
        public String getPrincipalOid() {
            return principal.getOid();
        }

        @Override
        public FocusType getPrincipalFocus() {
            return principal;
        }

        @Override
        public @NotNull Set<String> getSelfOids(@NotNull DelegatorSelection delegatorSelection) {
            return Set.of(principal.getOid());
        }

        @Override
        public @NotNull Set<String> getSelfPlusRolesOids(@NotNull DelegatorSelection delegatorSelection) {
            return Set.of(principal.getOid());
        }
    }

    private static class NoOpTracer implements ProcessingTracer<SelectorTraceEvent> {

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void trace(@NotNull SelectorTraceEvent event) {
        }
    }
}
