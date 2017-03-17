/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest.password;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Password test with HASHING storage for all credential types.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordDefaultHashing extends AbstractPasswordTest {
			
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Override
	protected String getSecurityPolicyOid() {
		return SECURITY_POLICY_DEFAULT_STORAGE_HASHING_OID;
	}
	
	@Override
	protected CredentialsStorageTypeType getPasswordStorageType() {
		return CredentialsStorageTypeType.HASHING;
	}

	@Override
	protected void assertShadowLifecycle(PrismObject<ShadowType> shadow, boolean focusCreated) {
		if (focusCreated) {
			assertShadowLifecycle(shadow, null);
		} else {
			assertShadowLifecycle(shadow, SchemaConstants.LIFECYCLE_PROPOSED);
		}
	}
	
}
