/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRunType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertReferenceValue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicCertificationTest extends AbstractCertificationTest {

    @Test
    public void test001StartCertification() throws Exception {

        final String TEST_NAME = "test001StartCertification";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationRunType certificationRunType =
                certificationManager.startCertificationRun(userRoleBasicCertificationType.asObjectable(), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("null created cert run", certificationRunType);
        UserType administrator = getUser(USER_ADMINISTRATOR_OID).asObjectable();
        display("administrator", administrator);
        MetadataType metadataType = administrator.getAssignment().get(0).getMetadata();
        assertNotNull("no metadata in administrator's assignment", metadataType);
        assertNotNull("no cert start time in administrator's assignment", metadataType.getCertificationStartedTimestamp());
        assertNull("unexpected cert finish time in administrator's assignment", metadataType.getCertificationFinishedTimestamp());
        assertReferenceValue(metadataType.asPrismContainerValue().findReference(MetadataType.F_CERTIFICATION_RUN_REF), certificationRunType.getOid());
        assertReferenceValue(metadataType.asPrismContainerValue().findReference(MetadataType.F_CERTIFIER_TO_DECIDE_REF), administrator.getOid());
        assertNull("unexpected certifier ref", metadataType.getCertifierRef());
        assertEquals("wrong cert status", CertificationStatusType.AWAITING_DECISION, metadataType.getCertificationStatus());
    }
}
