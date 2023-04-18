/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Collection;
import java.util.function.Consumer;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.schema.DeltaConvertor;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SimulationsBaselineTest extends SqaleRepoBaseTest {

    private static final String TEST_TAG_1 = "00000000-0000-0000-0000-000000001000";
    private static final String TEST_TAG_2 = "00000000-0000-0000-0000-000000002000";
    private static final String TEST_ARCHETYPE = "d029f4e9-d0e9-4486-a927-20585d909dc7";
    private static final String RANDOM_OID = "24a89c52-e477-4a38-b3c0-75a01d8c13f3";

    private static final String USER1_OID = "f333e8f2-b38a-4b13-b37e-b34b629d50f9";
    private static final String USER1_NAME = "user1";
    private static final String USER2_OID = "0d05c1ae-7687-4954-91e9-5a6af887e70e";
    private static final String USER2_NAME = "user2";
    private static final String SHADOW_OID = "76d44f44-1703-4acc-8b44-df01fe022203";
    private static final String SHADOW_NAME = "account";

    private String firstResultOid;
    private String secondResultOid;

    @Override
    @BeforeClass
    public void initDatabase() throws Exception {
        super.initDatabase();
        createResultsForSearching();
    }

    private void createResultsForSearching() throws SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        var user1 = new UserType()
                .oid(USER1_OID)
                .name(USER1_NAME);
        var user2 = new UserType()
                .oid(USER2_OID)
                .name(USER2_NAME);
        var shadow = new ShadowType()
                .oid(SHADOW_OID)
                .name(SHADOW_NAME);

        ObjectDeltaType addDeltaBean = DeltaConvertor.toObjectDeltaType(user1.clone().asPrismObject().createAddDelta());

        firstResultOid = repositoryService.addObject(
                new SimulationResultType()
                        .name("First Result")
                        .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                        .processedObject(new SimulationResultProcessedObjectType()
                                .transactionId("#1")
                                .oid(user1.getOid())
                                .name(user1.getName())
                                .type(UserType.COMPLEX_TYPE)
                                .state(ObjectProcessingStateType.ADDED)
                                .after(user1.clone())
                                .delta(addDeltaBean.clone()))
                        .asPrismObject(),
                null, result);

        secondResultOid = repositoryService.addObject(
                new SimulationResultType()
                        .name("Second Result")
                        .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                        .processedObject(new SimulationResultProcessedObjectType()
                                .transactionId("#1")
                                .oid(user1.getOid())
                                .name(user1.getName())
                                .type(UserType.COMPLEX_TYPE)
                                .state(ObjectProcessingStateType.ADDED)
                                .after(user1.clone())
                                .delta(addDeltaBean.clone()))
                        .processedObject(new SimulationResultProcessedObjectType()
                                .transactionId("#2")
                                .oid(user2.getOid())
                                .name(user2.getName())
                                .type(UserType.COMPLEX_TYPE)
                                .state(ObjectProcessingStateType.UNMODIFIED)
                                .before(user2.clone())
                                .after(user2.clone()))
                        .processedObject(new SimulationResultProcessedObjectType()
                                .transactionId("#3")
                                .oid(shadow.getOid())
                                .name(shadow.getName())
                                .type(ShadowType.COMPLEX_TYPE)
                                .state(ObjectProcessingStateType.UNMODIFIED)
                                .before(shadow.clone())
                                .after(shadow.clone()))
                        .asPrismObject(),
                null, result);
    }

    @Test
    public void test100CreateSimulation() throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();
        given("simulation result with a dummy system configuration object");
        SystemConfigurationType systemConfiguration = new SystemConfigurationType()
                .name("System Configuration")
                .description("dummy one");

        SimulationResultType obj = new SimulationResultType()
                .name("Test Simulation Result")
                .rootTaskRef(TEST_TAG_1, TaskType.COMPLEX_TYPE)
                .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                .archetypeRef(TEST_ARCHETYPE, ArchetypeType.COMPLEX_TYPE)
                .assignment(new AssignmentType()
                        .targetRef(TEST_ARCHETYPE, ArchetypeType.COMPLEX_TYPE))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("1")
                        .oid("00000000-0000-0000-0000-000000000001")
                        .name("System Configuration")
                        .state(ObjectProcessingStateType.UNMODIFIED)
                        .eventMarkRef(TEST_TAG_1, MarkType.COMPLEX_TYPE)
                        .eventMarkRef(TEST_TAG_2, MarkType.COMPLEX_TYPE)
                        .before(systemConfiguration.clone()))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("2")
                        .oid("00000000-0000-0000-0000-000000000002")
                        .name("Administrator")
                        .before(systemConfiguration.clone()));

        when("result is added to the repository");
        var simulationOid = repositoryService.addObject(obj.asPrismObject(), null, result);

        and("result is read back (as an object)");
        @NotNull PrismObject<SimulationResultType> resultReadBack =
                repositoryService.getObject(SimulationResultType.class, simulationOid, null, result);

        then("result is OK but empty - processed objects should are available only via search");
        assertNotNull(resultReadBack);
        assertTrue(resultReadBack.asObjectable().getProcessedObject().isEmpty());

        when("processed objects are retrieved explicitly");

        // And we search TEST_TAG_1 owned by created result

        ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(simulationOid)
                .and()
                .item(SimulationResultProcessedObjectType.F_EVENT_MARK_REF).ref(TEST_TAG_1)
                .build();
        SearchResultList<SimulationResultProcessedObjectType> processedObjects =
                repositoryService.searchContainers(SimulationResultProcessedObjectType.class, query, null, result);

        then("they are present");
        assertNotNull(processedObjects);
        assertThat(processedObjects).as("processed objects").hasSize(1);

        and("can be parsed");
        ObjectType objectBefore = processedObjects.get(0).getBefore();
        assertThat(objectBefore).as("'object before' from result").isEqualTo(systemConfiguration);

        when("simulation result is searched by matching archetypeRef");
        var byArchetypeRefMatching = repositoryService.searchObjects(
                SimulationResultType.class,
                prismContext.queryFor(SimulationResultType.class)
                        .item(SimulationResultType.F_ARCHETYPE_REF)
                        .ref(TEST_ARCHETYPE)
                        .build(),
                null, result);

        then("it is found");
        assertThat(byArchetypeRefMatching).as("by archetypeRef, matching").hasSize(1);

        when("simulation result is searched by matching assignment/targetRef");
        var byAssignmentTargetRefMatching = repositoryService.searchObjects(
                SimulationResultType.class,
                prismContext.queryFor(SimulationResultType.class)
                        .item(SimulationResultType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(TEST_ARCHETYPE)
                        .build(),
                null, result);

        then("it is found");
        assertThat(byAssignmentTargetRefMatching).as("by assignment/targetRef, matching").hasSize(1);

        when("simulation result is searched by non-matching archetypeRef");
        var byArchetypeRefNonMatching = repositoryService.searchObjects(
                SimulationResultType.class,
                prismContext.queryFor(SimulationResultType.class)
                        .item(SimulationResultType.F_ARCHETYPE_REF)
                        .ref(RANDOM_OID)
                        .build(),
                null, result);

        then("nothing is found");
        assertThat(byArchetypeRefNonMatching).as("by archetypeRef, non-matching").isEmpty();

        when("simulation result is searched by non-matching assignment/targetRef");
        var byAssignmentTargetRefNonMatching = repositoryService.searchObjects(
                SimulationResultType.class,
                prismContext.queryFor(SimulationResultType.class)
                        .item(SimulationResultType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(RANDOM_OID)
                        .build(),
                null, result);

        then("nothing is found");
        assertThat(byAssignmentTargetRefNonMatching).as("by assignment/targetRef, non-matching").isEmpty();
    }

    protected boolean getPartitioned() {
        return false;
    }

    @Test
    public void test110CreateTag() throws Exception {
        OperationResult result = createOperationResult();
        MarkType obj = new MarkType().name("testOfTest");
        String oid = repositoryService.addObject(obj.asPrismObject(), null, result);

        @NotNull
        PrismObject<MarkType> read = repositoryService.getObject(MarkType.class, oid, null, result);
        assertNotNull(read);
    }

    /** Deleting POs explicitly and using a delta. */
    @Test
    public void test200DeleteProcessedObjects() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("three simulation results in repository");
        var user = new UserType()
                .oid("d9fcaccc-9f38-437d-b26e-8e6715e47afc")
                .name("user");
        ObjectDeltaType addDeltaBean = DeltaConvertor.toObjectDeltaType(user.clone().asPrismObject().createAddDelta());

        SimulationResultType simResult1 = new SimulationResultType()
                .name("Result 1")
                .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("T1")
                        .oid(user.getOid())
                        .name(user.getName())
                        .state(ObjectProcessingStateType.ADDED)
                        .after(user.clone())
                        .delta(addDeltaBean));
        repositoryService.addObject(simResult1.asPrismObject(), null, result);

        SimulationResultType simResult2 = new SimulationResultType()
                .name("Result 2")
                .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("T1")
                        .oid(user.getOid())
                        .name(user.getName())
                        .state(ObjectProcessingStateType.UNMODIFIED)
                        .before(user.clone())
                        .after(user.clone()))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("T2")
                        .oid(user.getOid())
                        .name(user.getName())
                        .state(ObjectProcessingStateType.UNMODIFIED)
                        .before(user.clone())
                        .after(user.clone()));
        repositoryService.addObject(simResult2.asPrismObject(), null, result);

        SimulationResultType simResult3 = new SimulationResultType()
                .name("Result 3")
                .definition(new SimulationDefinitionType().useOwnPartitionForProcessedObjects(getPartitioned()))
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("T1")
                        .oid(user.getOid())
                        .name(user.getName())
                        .state(ObjectProcessingStateType.UNMODIFIED)
                        .before(user.clone())
                        .after(user.clone()));
        repositoryService.addObject(simResult3.asPrismObject(), null, result);

        when("POs in transaction T1 in first one are deleted (explicitly)");
        repositoryService.deleteSimulatedProcessedObjects(simResult1.getOid(), "T1", result);

        then("POs in first results are deleted, others are intact");
        assertProcessedObjects(simResult1.getOid(), 0, result);
        assertProcessedObjects(simResult2.getOid(), 2, result);
        assertProcessedObjects(simResult3.getOid(), 1, result);

        when("POs in first one are deleted (explicitly)");
        repositoryService.deleteSimulatedProcessedObjects(simResult1.getOid(), null, result);
        assertProcessedEmptyOrNoPartition(simResult1.getOid(), result);
        when("POs in second result are deleted (via delta)");
        var modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                .item(SimulationResultType.F_PROCESSED_OBJECT)
                .replace()
                .asItemDeltas();
        repositoryService.modifyObject(SimulationResultType.class, simResult2.getOid(), modifications, null, result);

        then("POs in first two results are deleted, the third one is intact");
        assertProcessedEmptyOrNoPartition(simResult1.getOid(), result);
        assertProcessedEmptyOrNoPartition(simResult2.getOid(), result);
        assertProcessedObjects(simResult3.getOid(), 1, result);
    }

    protected void assertProcessedEmptyOrNoPartition(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        assertProcessedObjects(oid, 0, result);
    }

    /** Searching for POs. */
    @Test
    public void test300SearchForProcessedObjects() throws SchemaException {
        OperationResult result = createOperationResult();

        when("checking 'search all'");
        ObjectQuery allFrom1st = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(firstResultOid)
                .build();
        checkCountAndSearch("all from 1st", allFrom1st, 1, result,
                po -> assertThat(po.getState())
                        .as("state")
                        .isEqualTo(ObjectProcessingStateType.ADDED));

        when("checking search by name");
        ObjectQuery byName =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_NAME)
                        .eq(USER2_NAME)
                        .build();
        checkCountAndSearch("by name", byName, 1, result,
                po -> assertThat(po.getName().getOrig())
                        .as("name")
                        .isEqualTo(USER2_NAME));

        when("checking search by OID");
        ObjectQuery byOid =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_OID)
                        .eq(USER1_OID)
                        .build();
        checkCountAndSearch("by OID", byOid, 1, result,
                po -> assertThat(po.getOid())
                        .as("OID")
                        .isEqualTo(USER1_OID));

        when("checking search by record ID");
        var firstId = getProcessedObjects(secondResultOid, result)
                .iterator().next().getId();
        ObjectQuery byId =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(PrismConstants.T_ID)
                        .eq(firstId)
                        .build();
        checkCountAndSearch("by ID", byId, 1, result,
                po -> assertThat(po.getId())
                        .as("ID")
                        .isEqualTo(firstId));

        when("checking search by transaction ID");
        ObjectQuery byTxId =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_TRANSACTION_ID)
                        .eq("#3")
                        .build();
        checkCountAndSearch("by tx ID", byTxId, 1, result,
                po -> assertThat(po.getTransactionId())
                        .as("tx ID")
                        .isEqualTo("#3"));

        when("checking search by state");
        ObjectQuery byState =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_STATE)
                        .eq(ObjectProcessingStateType.ADDED)
                        .build();
        checkCountAndSearch("by OID", byState, 1, result,
                po -> assertThat(po.getState())
                        .as("state")
                        .isEqualTo(ObjectProcessingStateType.ADDED));

        when("checking search by type");
        ObjectQuery byType =
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(secondResultOid)
                        .and()
                        .item(SimulationResultProcessedObjectType.F_TYPE)
                        .eq(ShadowType.COMPLEX_TYPE)
                        .build();
        checkCountAndSearch("by type", byType, 1, result,
                po -> assertThat(po.getType())
                        .as("type")
                        .isEqualTo(ShadowType.COMPLEX_TYPE));
    }

    @SuppressWarnings("SameParameterValue")
    private void checkCountAndSearch(
            String label, ObjectQuery query, int expectedCount, OperationResult result,
            Consumer<SimulationResultProcessedObjectType> asserter) throws SchemaException {
        when("counting POs " + label);
        int count = repositoryService.countContainers(SimulationResultProcessedObjectType.class, query, null, result);

        then("result is OK");
        assertThat(count).as("count " + label).isEqualTo(expectedCount);

        when("searching for POs " + label);
        var objects = repositoryService.searchContainers(SimulationResultProcessedObjectType.class, query, null, result);

        then("results are OK");
        assertThat(objects).as("objects " + label).hasSize(expectedCount);
        objects.forEach(asserter);
    }

    private void assertProcessedObjects(String oid, int expectedObjects, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        var simResult = repositoryService.getObject(SimulationResultType.class, oid, null, result);
        assertThat(simResult).as("simulation result").isNotNull();
        assertThat(getProcessedObjects(oid, result)).as("processed objects").hasSize(expectedObjects);
    }

    private Collection<SimulationResultProcessedObjectType> getProcessedObjects(String oid, OperationResult result)
            throws SchemaException {
        return repositoryService.searchContainers(
                SimulationResultProcessedObjectType.class,
                PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownerId(oid)
                        .build(),
                null,
                result);
    }
}
