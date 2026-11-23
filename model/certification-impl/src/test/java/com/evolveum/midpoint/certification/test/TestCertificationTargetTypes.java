/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.test;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests which assignment targets are certified by the direct assignment handler.
 * The user has assignments to an application, a policy and a role.
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCertificationTargetTypes extends AbstractCertificationTest {

    private static final File TEST_DIR = new File("src/test/resources/target-types");

    private static final File OBJECTS_FILE = new File(TEST_DIR, "objects.xml");
    private static final File CERT_DEF_ALL_FILE = new File(TEST_DIR, "cert-def-all.xml");
    private static final File CERT_DEF_NO_APPLICATIONS_FILE = new File(TEST_DIR, "cert-def-no-applications.xml");

    private static final String APPLICATION_OID = "c1a0de00-0000-4000-8000-000000000042";
    private static final String POLICY_OID = "c1a0de00-0000-4000-8000-000000000041";
    private static final String ROLE_OID = "c1a0de00-0000-4000-8000-000000000070";

    private static final String CERT_DEF_ALL_OID = "c1a0de00-0000-4000-8000-000000000072";
    private static final String CERT_DEF_NO_APPLICATIONS_OID = "c1a0de00-0000-4000-8000-000000000073";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectsFromFile(OBJECTS_FILE, ObjectType.class, initResult);
        repoAddObjectFromFile(CERT_DEF_ALL_FILE, AccessCertificationDefinitionType.class, initResult);
        repoAddObjectFromFile(CERT_DEF_NO_APPLICATIONS_FILE, AccessCertificationDefinitionType.class, initResult);
    }

    /** By default, all target types (including applications) are certified, and the campaign does not fail. */
    @Test
    public void test010ApplicationsIncludedByDefault() throws Exception {
        assertCertifiedTargets(CERT_DEF_ALL_OID, Set.of(APPLICATION_OID, POLICY_OID, ROLE_OID));
    }

    /** Applications can be excluded from the certification by the scope definition. */
    @Test
    public void test020ApplicationsExcluded() throws Exception {
        assertCertifiedTargets(CERT_DEF_NO_APPLICATIONS_OID, Set.of(POLICY_OID, ROLE_OID));
    }

    private void assertCertifiedTargets(String definitionOid, Set<String> expectedTargetOids) throws Exception {
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        AccessCertificationCampaignType campaign = certificationManager.createCampaign(definitionOid, task, result);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        certificationManager.createNextStageTask(campaign.getOid(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaign.getOid(), startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaignWithCases = getCampaignWithCases(campaign.getOid());
        display("campaign", campaignWithCases);

        Set<String> targetOids = campaignWithCases.getCase().stream()
                .map(c -> c.getTargetRef().getOid())
                .collect(Collectors.toSet());
        assertEquals("Wrong certified targets", expectedTargetOids, targetOids);
        assertCasesCount(campaign.getOid(), expectedTargetOids.size());
    }
}
