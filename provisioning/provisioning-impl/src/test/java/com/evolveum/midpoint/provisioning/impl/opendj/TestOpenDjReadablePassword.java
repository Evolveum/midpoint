/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Test for provisioning service implementation using embedded OpenDj instance.
 * This is the same test as TestOpenDj, but the configuration allows password reading.
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjReadablePassword extends TestOpenDj {

	protected static final File RESOURCE_OPENDJ_READABLE_PASSWORD_FILE = new File(TEST_DIR, "resource-opendj-readable-password.xml");
	
	private static Trace LOGGER = TraceManager.getTrace(TestOpenDjReadablePassword.class);
		
	@Override
	protected File getResourceOpenDjFile() {
		return RESOURCE_OPENDJ_READABLE_PASSWORD_FILE;
	}
	
	@Override
	protected void assertPasswordCapability(PasswordCapabilityType capPassword) {
		assertTrue("Wrong password capability readable flag: "+capPassword.isReadable(), 
				capPassword.isReadable() == Boolean.TRUE);
	}
	
	@Override
	protected void assertShadowPassword(ShadowType provisioningShadow) throws Exception {
		CredentialsType credentials = provisioningShadow.getCredentials();
		if (credentials == null) {
			return;
		}
		PasswordType passwordType = credentials.getPassword();
		if (passwordType == null) {
			return;
		}
		ProtectedStringType passwordValue = passwordType.getValue();
		assertNotNull("Missing password value in "+provisioningShadow, passwordValue);
		assertFalse("Empty password value in "+provisioningShadow, passwordValue.isEmpty());
		String clearPassword = protector.decryptString(passwordValue);
		display("Clear password of "+provisioningShadow+": "+clearPassword);
		
		PrismContainerValue<PasswordType> passwordContainer = passwordType.asPrismContainerValue();
		PrismProperty<ProtectedStringType> valueProp = passwordContainer.findProperty(PasswordType.F_VALUE);
		assertFalse("Incomplete password value in "+provisioningShadow, valueProp.isIncomplete());
	}
}
