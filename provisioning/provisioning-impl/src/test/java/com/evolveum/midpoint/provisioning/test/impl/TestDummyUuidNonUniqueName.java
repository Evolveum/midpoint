/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.provisioning.test.impl;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * Almost the same as TestDummy but this is using a UUID as ICF UID.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUuidNonUniqueName extends TestDummyUuid {
	
	public static final String TEST_DIR = "src/test/resources/impl/dummy-uuid-nonunique-name/";
	public static final String RESOURCE_DUMMY_FILENAME = TEST_DIR + "resource-dummy.xml";

	@Override
	protected String getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILENAME;
	}
	
	@Override
	protected boolean isNameUnique() {
		return false;
	}
		
}
