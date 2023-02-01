package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

public class SimulationsBaselineTest extends SqaleRepoBaseTest {

    private static final String TEST_TAG_1 = "00000000-0000-0000-0000-000000001000";
    private static final String TEST_TAG_2 = "00000000-0000-0000-0000-000000002000";
    private static final String TEST_ARCHETYPE = "d029f4e9-d0e9-4486-a927-20585d909dc7";
    private static final String RANDOM_OID = "24a89c52-e477-4a38-b3c0-75a01d8c13f3";

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
                        .targetRef(TEST_ARCHETYPE, ArchetypeType.COMPLEX_TYPE)
                )
                .processedObject(new SimulationResultProcessedObjectType()
                    .transactionId("1")
                    .oid("00000000-0000-0000-0000-000000000001")
                    .name("System Configuration")
                    .state(ObjectProcessingStateType.UNMODIFIED)
                    .eventMarkRef(TEST_TAG_1, MarkType.COMPLEX_TYPE)
                    .eventMarkRef(TEST_TAG_2, MarkType.COMPLEX_TYPE)
                    .before(systemConfiguration.clone())
                 )
                .processedObject(new SimulationResultProcessedObjectType()
                        .transactionId("2")
                        .oid("00000000-0000-0000-0000-000000000002")
                        .name("Administrator")
                        .before(systemConfiguration.clone())
                     )
                ;

        when("result is added to the repository");
        @NotNull String oid = repositoryService.addObject(obj.asPrismObject(), null, result);

        and("result is read back (as an object)");
        @NotNull PrismObject<SimulationResultType> resultReadBack =
                repositoryService.getObject(SimulationResultType.class, oid, null, result);

        then("result is OK but empty - processed objects should are available only via search");
        assertNotNull(resultReadBack);
        assertTrue(resultReadBack.asObjectable().getProcessedObject().isEmpty());

        when("processed objects are retrieved explicitly");

        // And we search TEST_TAG_1 owned by created result

        ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(oid)
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
}
