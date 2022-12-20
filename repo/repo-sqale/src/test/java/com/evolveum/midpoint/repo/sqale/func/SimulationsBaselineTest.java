package com.evolveum.midpoint.repo.sqale.func;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

public class SimulationsBaselineTest extends SqaleRepoBaseTest {


    @Test
    public void test100CreateSimulation() throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();
        SimulationResultType obj = new SimulationResultType()
                .name("Test Simulation Result")
                .processedObject(new SimulationResultProcessedObjectType()
                    .oid("00000000-0000-0000-0000-000000000001")
                    .name("System Configuration")
                    .state(ObjectProcessingStateType.UNMODIFIED)
                    .metricIdentifier("disabled")
                    .metricIdentifier("business")
                 );
        @NotNull
        String oid = repositoryService.addObject(obj.asPrismObject(), null, result);

        @NotNull
        PrismObject<SimulationResultType> readed = repositoryService.getObject(SimulationResultType.class, oid, null, result);
        assertNotNull(readed);

        // Processed objects should not be fetched from repository (available only via search)
        assertTrue(readed.asObjectable().getProcessedObject().isEmpty());

        SearchResultList<SimulationResultProcessedObjectType> ret = repositoryService.searchContainers(SimulationResultProcessedObjectType.class, null, null, result);
        assertNotNull(ret);
    }

    @Test
    public void test110createTag() throws Exception {
        OperationResult result = createOperationResult();
        TagType obj = new TagType().name("testOfTest");
        String oid = repositoryService.addObject(obj.asPrismObject(), null, result);

        @NotNull
        PrismObject<TagType> readed = repositoryService.getObject(TagType.class, oid, null, result);
        assertNotNull(readed);
    }
}
