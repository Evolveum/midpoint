/*
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Almost the same as TestDummy but using incomplete attribute flags
 * 
 * MID-3573
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyIncomplete extends TestDummy {
	
	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-incomplete");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}
	
	@Override
	protected int getExpectedRefinedSchemaDefinitions() {
		return super.getExpectedRefinedSchemaDefinitions() + 1;
	}
	
	@Override
	protected void assertNativeCredentialsCapability(CredentialsCapabilityType capCred) {
		PasswordCapabilityType passwordCapabilityType = capCred.getPassword();
		assertNotNull("password native capability not present", passwordCapabilityType);
		Boolean readable = passwordCapabilityType.isReadable();
		assertNotNull("No 'readable' inducation in password capability", readable);
		assertTrue("Password not 'readable' in password capability", readable);
	}

	@Override
	protected void checkAccountWill(PrismObject<ShadowType> shadow, OperationResult result, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) throws SchemaException, EncryptionException {
		super.checkAccountWill(shadow, result, startTs, endTs);
		CredentialsType credentials = shadow.asObjectable().getCredentials();
		assertNotNull("No credentials in "+shadow, credentials);
		PasswordType password = credentials.getPassword();
		assertNotNull("No password in "+shadow, password);
		PrismContainerValue<PasswordType> passwordContainerValue = password.asPrismContainerValue();
		PrismProperty<ProtectedStringType> valueProperty = passwordContainerValue.findProperty(PasswordType.F_VALUE);
		assertTrue("Unexpected password value in "+shadow+": "+valueProperty, valueProperty.getValues().isEmpty());
		assertTrue("No incompleteness in password value in "+shadow+": "+valueProperty, valueProperty.isIncomplete());
	}
	
	@Test
	public void testFakeToEnableDebug() {
		
	}
	
}
