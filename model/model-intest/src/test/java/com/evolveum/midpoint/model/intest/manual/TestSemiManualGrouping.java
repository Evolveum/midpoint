/*
 * Copyright (c) 2010-2018 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * MID-4347
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualGrouping extends AbstractGroupingManualResourceTest {

	@Override
	protected BackingStore createBackingStore() {
		return new CsvBackingStore();
	}
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_GROUPING_FILE;
	}

	@Override
	protected String getRoleOneOid() {
		return ROLE_ONE_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_SEMI_MANUAL_GROUPING_FILE;
	}

	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_SEMI_MANUAL_GROUPING_FILE;
	}
	
	@Override
	protected String getPropagationTaskOid() {
		return TASK_PROPAGATION_SEMI_MANUAL_GROUPING_OID;
	}

	@Override
	protected File getPropagationTaskFile() {
		return TASK_PROPAGATION_SEMI_MANUAL_GROUPING_FILE;
	}

	@Override
	protected boolean hasMultivalueInterests() {
		return false;
	}

	@Override
	protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
		AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
	}

	@Override
	protected int getNumberOfAccountAttributeDefinitions() {
		return 5;
	}
	
	@Override
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// CSV password is readable
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password value property in "+shadow+": "+passValProp, passValProp);
	}

}