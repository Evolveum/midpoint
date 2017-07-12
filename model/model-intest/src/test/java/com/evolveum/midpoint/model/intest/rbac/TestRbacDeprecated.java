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
package com.evolveum.midpoint.model.intest.rbac;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbacDeprecated extends TestRbac {
	
	protected static final File ROLE_GOVERNOR_DEPRECATED_FILE = new File(TEST_DIR, "role-governor-deprecated.xml");
	protected static final File ROLE_CANNIBAL_DEPRECATED_FILE = new File(TEST_DIR, "role-cannibal-deprecated.xml");
		
	@Override
	protected File getRoleGovernorFile() {
		return ROLE_GOVERNOR_DEPRECATED_FILE;
	}

	@Override
	protected File getRoleCannibalFile() {
		return ROLE_CANNIBAL_DEPRECATED_FILE;
	}

	@Override
	protected boolean testMultiplicityConstraintsForNonDefaultRelations() {
		return false;
	}
}
