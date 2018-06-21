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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Almost the same as TestDummyCaseIgnore but the resource is changing all names to upper case.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyCaseIgnoreUpcaseName extends TestDummyCaseIgnore {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-case-ignore-upcase-name");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected String transformNameFromResource(String origName) {
		return origName.toUpperCase();
	}

	@Override
	protected String transformNameToResource(String origName) {
		return origName.toUpperCase();
	}

	@Override
	protected void assertShadowName(PrismObject<ShadowType> shadow, String expectedName) {
		assertEquals("Shadow name is wrong in "+shadow, expectedName.toLowerCase(), shadow.asObjectable().getName().getOrig().toLowerCase());
	}

}
