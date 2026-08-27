/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.archetypes;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.CollectionStats;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.DirectlyEvaluatedClockworkPolicyRule;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Test behavior of connectors that have several instances (poolable connectors).
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCollections extends AbstractArchetypesTest {

    private PrismObject<ObjectCollectionType> collectionActiveUsers;
    private CollectionRefSpecificationType collectionSpecActiveUsers;
    private CompiledObjectCollectionView collectionViewActiveUsers;
    private int numberOfDisabledUsers = 0;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(COLLECTION_ACTIVE_USERS_FILE, initResult);

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, initTask, initResult);
        // Make sure effectiveStatus and other attributes are OK
        recomputeAndRefreshObjects(users.stream().map(o -> o.asObjectable()).collect(Collectors.toList()), initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        collectionActiveUsers = modelService.getObject(ObjectCollectionType.class, COLLECTION_ACTIVE_USERS_OID, null, task, result);
        collectionSpecActiveUsers = new CollectionRefSpecificationType();
        collectionSpecActiveUsers.setCollectionRef(MiscSchemaUtil.createObjectReference(COLLECTION_ACTIVE_USERS_OID, ObjectCollectionType.COMPLEX_TYPE));

        // THEN
        then();
        display("Collection", collectionActiveUsers);
        assertSuccess(result);
        assertNotNull("No collection", collectionActiveUsers);
        assertNotNull("No collection specification", collectionSpecActiveUsers);
    }

    @Test
    public void test100CompileCollectionView() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        collectionViewActiveUsers = modelInteractionService.compileObjectCollectionView(collectionSpecActiveUsers, null, task, result);

        // THEN
        then();
        displayDumpable("Active users collection view", collectionViewActiveUsers);
        assertSuccess(result);
        assertNotNull("Null view", collectionActiveUsers);

        assertObjectCollectionView(collectionViewActiveUsers)
                .assertFilter()
                .assertDomainFilter();

    }

    /**
     * All users are enabled.
     */
    @Test
    public void test102SearchCollectionUsers() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, prismContext.queryFactory().createQuery(collectionViewActiveUsers.getFilter()), null, task, result);

        // THEN
        then();
        display("Users in collection", users);
        assertSuccess(result);
        assertEquals("Wrong number of users in collection", getNumberOfUsers(), users.size());
    }

    /**
     * MID-9879: Inline collection filter can be evaluated when the collection spec provides its object type.
     * The explicit type is also allowed to narrow a broader containing target type.
     */
    @Test
    public void test103CompileInlineTypedCollectionView() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter enabledUsersFilter = prismContext.queryFor(UserType.class)
                .item(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ENABLED)
                .buildFilter();
        CollectionRefSpecificationType inlineTypedCollection = new CollectionRefSpecificationType()
                .type(UserType.COMPLEX_TYPE)
                .filter(prismContext.getQueryConverter().createSearchFilterType(enabledUsersFilter));

        // WHEN
        when();
        CompiledObjectCollectionView inlineTypedCollectionView =
                modelInteractionService.compileObjectCollectionView(inlineTypedCollection, null, task, result);
        CompiledObjectCollectionView inlineTypedCollectionViewFromFocus =
                modelInteractionService.compileObjectCollectionView(inlineTypedCollection, FocusType.class, task, result);
        CollectionStats stats = modelInteractionService.determineCollectionStats(inlineTypedCollectionView, task, result);

        // THEN
        then();
        displayDumpable("Inline typed collection view", inlineTypedCollectionView);
        assertSuccess(result);

        assertObjectCollectionView(inlineTypedCollectionView)
                .assertFilter();
        assertEquals("Wrong container type", UserType.COMPLEX_TYPE, inlineTypedCollectionView.getContainerType());
        assertEquals("Wrong narrowed container type", UserType.COMPLEX_TYPE, inlineTypedCollectionViewFromFocus.getContainerType());
        assertEquals("Wrong target class", UserType.class, inlineTypedCollectionView.getTargetClass());
        assertEquals("Wrong narrowed target class", UserType.class, inlineTypedCollectionViewFromFocus.getTargetClass());
        assertEquals("Wrong object count", (Integer) getNumberOfUsers(), stats.getObjectCount());
    }

    /**
     * MID-9879: Inline collection type must not conflict with the containing target type.
     */
    @Test
    public void test104RejectConflictingInlineCollectionType() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter enabledUsersFilter = prismContext.queryFor(UserType.class)
                .item(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ENABLED)
                .buildFilter();
        CollectionRefSpecificationType inlineTypedCollection = new CollectionRefSpecificationType()
                .type(UserType.COMPLEX_TYPE)
                .filter(prismContext.getQueryConverter().createSearchFilterType(enabledUsersFilter));

        // WHEN
        when();
        assertConflictingInlineCollectionType(inlineTypedCollection, RoleType.class, task, result);

        CollectionRefSpecificationType inlineBroaderTypedCollection = inlineTypedCollection.clone()
                .type(FocusType.COMPLEX_TYPE);
        assertConflictingInlineCollectionType(inlineBroaderTypedCollection, UserType.class, task, result);
    }

    @Test
    public void test110CollectionStatsAllEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        CollectionStats stats = modelInteractionService.determineCollectionStats(collectionViewActiveUsers, task, result);

        // THEN
        then();
        displayValue("Collection stats", stats);
        assertSuccess(result);
        assertNotNull("Null stats", stats);

        assertEquals("Wrong object count", (Integer) getNumberOfUsers(), stats.getObjectCount());
        assertEquals("Wrong domain count", (Integer) getNumberOfUsers(), stats.getDomainCount());
        assertPercentage(stats, 100);
    }

    @Test
    public void test112EvaluateRulesAllEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Collection<DirectlyEvaluatedClockworkPolicyRule> evaluatedRules = modelInteractionService.evaluateCollectionPolicyRules(collectionActiveUsers, collectionViewActiveUsers, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEvaluatedPolicyRules(evaluatedRules, collectionActiveUsers)
                .single()
                .assertNotTriggered();
    }

    @Test
    public void test120CollectionStatsOneDisabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_GUYBRUSH_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);
        numberOfDisabledUsers++;

        // WHEN
        when();
        CollectionStats stats = modelInteractionService.determineCollectionStats(collectionViewActiveUsers, task, result);

        // THEN
        then();
        displayValue("Collection stats", stats);
        assertSuccess(result);
        assertNotNull("Null stats", stats);

        assertEquals("Wrong object count", (Integer) (getNumberOfUsers() - numberOfDisabledUsers), stats.getObjectCount());
        assertEquals("Wrong domain count", (Integer) getNumberOfUsers(), stats.getDomainCount());
        assertPercentage(stats, (getNumberOfUsers() - numberOfDisabledUsers) * 100f / getNumberOfUsers());
    }

    @Test
    public void test122EvaluateRulesOneDisabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        Collection<DirectlyEvaluatedClockworkPolicyRule> evaluatedRules = modelInteractionService.evaluateCollectionPolicyRules(collectionActiveUsers, collectionViewActiveUsers, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEvaluatedPolicyRules(evaluatedRules, collectionActiveUsers)
                .single()
                .assertPolicySituation(POLICY_SITUATION_TOO_MANY_INACTIVE_USERS)
                .singleTrigger()
                .assertConstraintKind(PolicyConstraintKindType.COLLECTION_STATS);

    }

    private void assertConflictingInlineCollectionType(
            CollectionRefSpecificationType inlineTypedCollection, Class<? extends Containerable> targetTypeClass,
            Task task, OperationResult result)
            throws Exception {

        try {
            modelInteractionService.compileObjectCollectionView(inlineTypedCollection, targetTypeClass, task, result);
        } catch (ConfigurationException e) {
            displayExpectedException(e);
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("Conflicting collection types"));
            return;
        }
        throw new AssertionError("Expected conflicting inline collection type to be rejected for "
                + targetTypeClass.getSimpleName());
    }
}
