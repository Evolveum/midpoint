/**
 * Copyright (c) 2018 Evolveum
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

import java.util.List;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractAdLdapMultidomainRunAsTest extends AbstractAdLdapMultidomainTest {
	
	@Override
	protected void assertAdditionalCapabilities(List<Object> nativeCapabilities) {
		super.assertAdditionalCapabilities(nativeCapabilities);
		
		assertCapability(nativeCapabilities, RunAsCapabilityType.class);
	}
	
	/**
	 * Try to set the same password again. If this is "admin mode" (no runAs capability - in superclass)
	 * the such change should be successful. In "selfservice mode" (runAs capability)
	 * this change should fail.
	 */
	@Test
	@Override
    public void test222ModifyUserBarbossaPasswordSelfServicePassword1Again() throws Exception {
		final String TEST_NAME = "test222ModifyUserBarbossaPasswordSelfServicePassword1Again";
		testModifyUserBarbossaPasswordSelfServiceFailure(TEST_NAME, USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_1);
        displayTestTitle(TEST_NAME);
	}

	/**
	 * Change password back to the first password. This password was used before.
	 * In admin mode (in superclass) this should go well. Admin can set password to anything.
	 * But in self-service mode this should fail due to password history check.
	 */
	@Test
	@Override
    public void test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain() throws Exception {
		final String TEST_NAME = "test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain";
		testModifyUserBarbossaPasswordSelfServiceFailure(TEST_NAME, USER_BARBOSSA_PASSWORD_AD_2, USER_BARBOSSA_PASSWORD_AD_1);
	}

}
