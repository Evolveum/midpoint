/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.CollectionStats;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.FailableRunnable;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test behavior of connectors that have several instances (poolable connectors).
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCollections extends AbstractArchetypesTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestCollections.class);

    private PrismObject<ObjectCollectionType> collectionActiveUsers;
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
        final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        collectionActiveUsers = modelService.getObject(ObjectCollectionType.class, COLLECTION_ACTIVE_USERS_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Collection", collectionActiveUsers);
        assertSuccess(result);
        assertNotNull("No collection", collectionActiveUsers);
    }

    @Test
    public void test100CompileCollectionView() throws Exception {
        final String TEST_NAME = "test100CompileCollectionView";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        collectionViewActiveUsers = modelInteractionService.compileObjectCollectionView(collectionActiveUsers, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Active users collection view", collectionViewActiveUsers);
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
        final String TEST_NAME = "test102SearchCollectionUsers";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, prismContext.queryFactory().createQuery(collectionViewActiveUsers.getFilter()), null, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Users in collection", users);
        assertSuccess(result);
        assertEquals("Wrong number of users in collection", getNumberOfUsers(), users.size());
    }

    @Test
    public void test110CollectionStatsAllEnabled() throws Exception {
        final String TEST_NAME = "test110CollectionStatsAllEnabled";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        CollectionStats stats = modelInteractionService.determineCollectionStats(collectionViewActiveUsers, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Collection stats", stats);
        assertSuccess(result);
        assertNotNull("Null stats", stats);

        assertEquals("Wrong object count", getNumberOfUsers(), stats.getObjectCount());
        assertEquals("Wrong domain count", (Integer)getNumberOfUsers(), stats.getDomainCount());
        assertPercentage(stats, 100);
    }

    @Test
    public void test112EvaluateRulesAllEnabled() throws Exception {
        final String TEST_NAME = "test112EvaluateRulesAllEnabled";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        Collection<EvaluatedPolicyRule> evaluatedRules = modelInteractionService.evaluateCollectionPolicyRules(collectionActiveUsers, collectionViewActiveUsers, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertEvaluatedPolicyRules(evaluatedRules, collectionActiveUsers)
            .single()
                .assertNotTriggered();
    }

    @Test
    public void test120CollectionStatsOneDisabled() throws Exception {
        final String TEST_NAME = "test120CollectionStatsOneDisabled";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        modifyUserReplace(USER_GUYBRUSH_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);
        numberOfDisabledUsers++;

        // WHEN
        displayWhen(TEST_NAME);
        CollectionStats stats = modelInteractionService.determineCollectionStats(collectionViewActiveUsers, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Collection stats", stats);
        assertSuccess(result);
        assertNotNull("Null stats", stats);

        assertEquals("Wrong object count", getNumberOfUsers() - numberOfDisabledUsers, stats.getObjectCount());
        assertEquals("Wrong domain count", (Integer)getNumberOfUsers(), stats.getDomainCount());
        assertPercentage(stats, (Float)((getNumberOfUsers() - numberOfDisabledUsers)*100f)/getNumberOfUsers());
    }

    @Test
    public void test122EvaluateRulesOneDisabled() throws Exception {
        final String TEST_NAME = "test122EvaluateRulesOneDisabled";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        Collection<EvaluatedPolicyRule> evaluatedRules = modelInteractionService.evaluateCollectionPolicyRules(collectionActiveUsers, collectionViewActiveUsers, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertEvaluatedPolicyRules(evaluatedRules, collectionActiveUsers)
            .single()
                .assertPolicySituation(POLICY_SITUATION_TOO_MANY_INACTIVE_USERS)
                .singleTrigger()
                    .assertConstraintKind(PolicyConstraintKindType.COLLECTION_STATS);

    }
}
