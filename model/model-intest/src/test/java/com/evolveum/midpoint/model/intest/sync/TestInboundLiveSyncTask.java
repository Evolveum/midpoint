/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.FileNotFoundException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundLiveSyncTask extends AbstractInboundSyncTest {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		dummyResourceEmerald.setSyncStyle(DummySyncStyle.SMART);
	}

	@Override
	protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
		if (resource == resourceDummyEmerald) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_EMERALD_FILE);
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@Override
	protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
		if (resource == resourceDummyEmerald) {
			return TASK_LIVE_SYNC_DUMMY_EMERALD_OID;
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@Override
	public void test199DeleteDummyEmeraldAccountMancomb() throws Exception {
		final String TEST_NAME = "test199DeleteDummyEmeraldAccountMancomb";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

		dummyResourceEmerald.deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountMancomb);
        assertNull("Account shadow mancomb not gone", accountMancomb);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb is gone", userMancomb);
        assertLinks(userMancomb, 0);
        // Disabled by sync reaction
        assertAdministrativeStatusDisabled(userMancomb);
//        assertNull("Unexpected valid from in user", userMancomb.asObjectable().getActivation().getValidFrom());
//        assertNull("Unexpected valid to in user", userMancomb.asObjectable().getActivation().getValidTo());
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertNoDummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);

	}

}
