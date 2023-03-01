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

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.schema.DeltaConvertor;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;
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
    private @NotNull String simulationOid;

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
        simulationOid = repositoryService.addObject(obj.asPrismObject(), null, result);

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
        // TODO this should work, shouldn't it?
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

    @Test
    public void test109deleteProcessedObjectsViaDelta() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        var delta = PrismContext.get().deltaFactory().object()
                .createEmptyModifyDelta(SimulationResultType.class, simulationOid);
        var cd = delta.createContainerModification(SimulationResultType.F_PROCESSED_OBJECT);
        cd.setValuesToReplace(new ArrayList<>());

        repositoryService.modifyObject(SimulationResultType.class, simulationOid, delta.getModifications(), null, createOperationResult());

    }

    protected boolean getPartitioned() {
        return false;
    }

    @Test
    public void test110createTag() throws Exception {
        OperationResult result = createOperationResult();
        MarkType obj = new MarkType().name("testOfTest");
        String oid = repositoryService.addObject(obj.asPrismObject(), null, result);

        @NotNull
        PrismObject<MarkType> read = repositoryService.getObject(MarkType.class, oid, null, result);
        assertNotNull(read);
    }

    @Test
    public void test200DeleteTransaction() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("two simulation results in repository");
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
                        .after(user.clone()));
        repositoryService.addObject(simResult2.asPrismObject(), null, result);

        when("POs in transaction T1 in first one are deleted");
        repositoryService.deleteSimulatedProcessedObjects(simResult1.getOid(), "T1", result);

        then("there are two results, one with POs, one with none");
        assertProcessedObjects(simResult1.getOid(), 0, result);
        assertProcessedObjects(simResult2.getOid(), 1, result);
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
