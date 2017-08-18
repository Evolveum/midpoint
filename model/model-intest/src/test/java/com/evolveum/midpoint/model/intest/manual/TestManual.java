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

/**
 * 
 */
package com.evolveum.midpoint.model.intest.manual;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestManual extends AbstractManualResourceTest {
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_MANUAL_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_MANUAL_FILE;
	}
	
	@Override
	protected String getRoleOneOid() {
		return ROLE_ONE_MANUAL_OID;
	}
	
	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_MANUAL_FILE;
	}
	
	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_MANUAL_OID;
	}
	
	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_MANUAL_FILE;
	}
	
	@Override
	protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
		AssertJUnit.assertNotNull("No schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
	}

}