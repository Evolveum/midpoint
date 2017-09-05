/*
 * Copyright (c) 2013-2017 Evolveum
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

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Almost the same as TestDummy but this is using a UUID as ICF UID.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUuid extends TestDummy {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-uuid");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected boolean isIcfNameUidSame() {
		return false;
	}

	@Override
	protected boolean isNameUnique(){
		return false;
	}

}
