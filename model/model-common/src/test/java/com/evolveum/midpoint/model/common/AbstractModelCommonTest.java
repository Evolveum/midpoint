/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.model.common;

import java.io.File;

import com.evolveum.midpoint.test.util.TestUtil;

/**
 * @author semancik
 *
 */
public class AbstractModelCommonTest {
	
	protected static final File COMMON_DIR = new File("src/test/resources/common");

	protected static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	protected static final String EXPRESSION_PROFILE_SAFE_NAME = "safe";

	protected void displayTestTitle(final String TEST_NAME) {
		TestUtil.displayTestTitle(this, TEST_NAME);
	}
	
	protected void displayWhen(final String TEST_NAME) {
		TestUtil.displayWhen(TEST_NAME);
	}
	
	protected void displayThen(final String TEST_NAME) {
		TestUtil.displayThen(TEST_NAME);
	}

}
