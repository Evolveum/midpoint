/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.ninja;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the recompute task that ninja creates at the end of the initial-objects action.
 */
@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class InitialObjectsTest extends NinjaSpringTest {

    private static final String OID_SYSTEM_CONFIGURATION = SystemObjectsType.SYSTEM_CONFIGURATION.value();
    private static final String OID_ROLE_SUPERUSER = SystemObjectsType.ROLE_SUPERUSER.value();

    @Test
    public void test100RecomputeTaskUsesRecomputationActivity() throws Exception {
        given();
        OperationResult result = new OperationResult("test100RecomputeTaskUsesRecomputationActivity");
        assertThat(repository.countObjects(TaskType.class, null, null, result)).isZero();

        when();
        executeTest(
                NOOP_STREAM_VALIDATOR,
                EMPTY_STREAM_VALIDATOR,
                "-m", getMidpointHome(),
                "initial-objects",
                "--force-add",
                "--oid", OID_ROLE_SUPERUSER, OID_SYSTEM_CONFIGURATION);

        then();
        assertThat(repository.getObject(RoleType.class, OID_ROLE_SUPERUSER, null, result)).isNotNull();
        assertThat(repository.getObject(SystemConfigurationType.class, OID_SYSTEM_CONFIGURATION, null, result)).isNotNull();

        SearchResultList<PrismObject<TaskType>> tasks = repository.searchObjects(TaskType.class, null, null, result);
        assertThat(tasks).hasSize(1);
        TaskType task = tasks.get(0).asObjectable();

        assertThat(task.getExecutionState()).isEqualTo(TaskExecutionStateType.RUNNABLE);
        assertThat(task.getOwnerRef().getOid()).isEqualTo(SystemObjectsType.USER_ADMINISTRATOR.value());
        assertThat(ObjectTypeUtil.getAssignedArchetypeOids(task))
                .containsExactly(SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value());

        WorkDefinitionsType work = task.getActivity().getWork();
        RecomputationWorkDefinitionType recomputation = work.getRecomputation();
        assertThat(recomputation).isNotNull();

        ObjectSetType objects = recomputation.getObjects();
        assertThat(objects.getType()).isEqualTo(AssignmentHolderType.COMPLEX_TYPE);
        ObjectFilter filter = prismContext.getQueryConverter()
                .parseFilter(objects.getQuery().getFilter(), AssignmentHolderType.class);
        assertThat(filter).isInstanceOf(InOidFilter.class);
        assertThat(((InOidFilter) filter).getOids())
                .containsExactlyInAnyOrder(OID_ROLE_SUPERUSER, OID_SYSTEM_CONFIGURATION);
    }

    @Test
    public void test200NoTaskWhenNothingChanged() throws Exception {
        given();
        OperationResult result = new OperationResult("test200NoTaskWhenNothingChanged");
        assertThat(repository.countObjects(TaskType.class, null, null, result)).isEqualTo(1);

        when();
        executeTest(
                NOOP_STREAM_VALIDATOR,
                EMPTY_STREAM_VALIDATOR,
                "-m", getMidpointHome(),
                "initial-objects",
                "--force-add",
                "--oid", OID_ROLE_SUPERUSER, OID_SYSTEM_CONFIGURATION);

        then();
        assertThat(repository.countObjects(TaskType.class, null, null, result)).isEqualTo(1);
    }
}
