/**
 * Copyright (c) 2015-2019 Evolveum
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
package com.evolveum.midpoint.testing.conntest.ad;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapChimeraRunAs extends TestAdLdapChimera {

	@Override
	protected String getResourceOid() {
		return "eced6d24-73e3-11e5-8457-93eff15a6b85";
	}

	@Override
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource-chimera-runas.xml");
	}

	/**
	 * Try to set the same password again. In "selfservice mode" (runAs capability)
	 * this change should fail.
	 */
	@Override
	@Test
    public void test222ModifyUserBarbossaPasswordSelfServicePassword1Again() throws Exception {
		final String TEST_NAME = "test222ModifyUserBarbossaPasswordSelfServicePassword1Again";
		testModifyUserBarbossaPasswordSelfServiceFailure(TEST_NAME, USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_1);
	}
	
	/**
	 * Change password back to the first password. This password was used before.
	 * In self-service mode (in subclass) this should fail due to password history check.
	 */
	@Override
	@Test
    public void test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain() throws Exception {
		final String TEST_NAME = "test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain";
		testModifyUserBarbossaPasswordSelfServiceFailure(TEST_NAME, USER_BARBOSSA_PASSWORD_AD_2, USER_BARBOSSA_PASSWORD_AD_1);
	}
}
