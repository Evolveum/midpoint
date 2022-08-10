/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingRequest;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Testing identity matching using real ID Match implementation (currently COmanage Match).
 *
 * REQUIREMENTS:
 *
 * 1) The COmanage Match runs as an external system. See `resource-ais.xml` for URL and credentials.
 * 2) The COmanage Match is configured like {@link DummyIdMatchServiceImpl#executeMatch(MatchingRequest, OperationResult)}.
 * 3) The database of the service is initially empty (no records).
 *
 * This test runs manually.
 */
public class TestIdMatchCorrelationMultiAccountsReal extends AbstractMultiAccountsIdMatchCorrelationTest {

    // No ID Match Service override here, so the URL configured in the resource will be used.

    /**
     * Attempt to correlate using wrong credentials.
     */
    @Test
    public void test900WrongCredentials() throws CommonException, IOException {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RESOURCE_SIS.appendLine("900,Nobody,Nobody,0000-00-00,000000/0000,sw-eng");

        breakCredentials(task, result);

        when("running the import task");
        TASK_IMPORT_SIS.rerunErrorsOk(result);

        then("checking that authentication error is properly recorded");

        // Adapt this if the details of error handling in synchronization service are changed.
        assertFailure("900", "Error occurred during resource object shadow owner lookup, reason: "
                + "ID Match service returned 401: Unauthorized");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertFailure(String accountName, String message) throws CommonException {
        OperationResult result = getTestOperationResult();

        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .rootActivityState()
                    .progress()
                        .assertFailureCount(1, false)
                    .end()
                    .itemProcessingStatistics()
                        .assertFailureCount(1)
                        .assertLastFailureObjectName(accountName)
                        .assertLastFailureMessageContaining(message);

        PrismObject<ShadowType> shadow = findShadowByPrismName(accountName, RESOURCE_SIS.getObject(), result);
        assertShadowAfter(shadow)
                .assertCorrelationSituation(CorrelationSituationType.ERROR)
                .assertHasComplexOperationExecutionFailureWithMessageContaining(TASK_IMPORT_SIS.oid, message);
        // @formatter:on
    }

    private void breakCredentials(Task task, OperationResult result) throws CommonException {
        executeChanges(
                deltaFor(ResourceType.class)
                        .item(getConfigurationPath().append(IdMatchCorrelatorType.F_PASSWORD))
                        .replace(new ProtectedStringType().clearValue("nonsense"))
                        .asObjectDelta(RESOURCE_SIS.oid),
                null, task, result);
    }

    /**
     * Attempt to correlate using wrong server name.
     *
     * Expects "900" account created in previous test.
     */
    @Test
    public void test910WrongServerName() throws CommonException {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        deleteObjectRepo(
                ShadowType.class,
                findShadowByPrismName("900", RESOURCE_SIS.getObject(), result).getOid());

        breakServerAddress(task, result);

        when("running the import task");
        TASK_IMPORT_SIS.rerunErrorsOk(result);

        then("checking that connection error is properly recorded");

        // Adapt this if the details of error handling in synchronization service are changed.
        assertFailure("900", "Error occurred during resource object shadow owner lookup, reason: "
                + "Couldn't invoke ID Match service:");
    }

    private void breakServerAddress(Task task, OperationResult result) throws CommonException {
        executeChanges(
                deltaFor(ResourceType.class)
                        .item(getConfigurationPath().append(IdMatchCorrelatorType.F_URL))
                        .replace("http://xxxxxxxxxxxx:9090/match/api/1")
                        .asObjectDelta(RESOURCE_SIS.oid),
                null, task, result);
    }

    private @NotNull ItemPath getConfigurationPath() {
        ResourceObjectTypeDefinitionType definitionBean = RESOURCE_SIS.getObjectable()
                .getSchemaHandling()
                .getObjectType()
                .get(0);
        long idMatchId = definitionBean
                .getCorrelation()
                .getCorrelators()
                .getIdMatch().get(0).getId();
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                definitionBean.getId(),
                ResourceObjectTypeDefinitionType.F_CORRELATION,
                CorrelationDefinitionType.F_CORRELATORS,
                CompositeCorrelatorType.F_ID_MATCH,
                idMatchId);
    }
}
