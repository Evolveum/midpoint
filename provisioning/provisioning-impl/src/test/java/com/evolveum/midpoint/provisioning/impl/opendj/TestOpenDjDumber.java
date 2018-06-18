/*
 * Copyright (c) 2016 Evolveum
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

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Test for provisioning service implementation using embedded OpenDj instance.
 * This is the same test as TestOpenDj, but the OpenDJ resource configuration is
 * somehow dumber: no shortcut in associations, expclicit duplicity checks, etc.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjDumber extends TestOpenDj {

	protected static final File RESOURCE_OPENDJ_DUMBER_FILE = new File(TEST_DIR, "resource-opendj-dumber.xml");

	private static Trace LOGGER = TraceManager.getTrace(TestOpenDjDumber.class);

	@Override
	protected File getResourceOpenDjFile() {
		return RESOURCE_OPENDJ_DUMBER_FILE;
	}

	@Override
	protected int getNumberOfBaseContextShadows() {
		return 1;
	}

	@Override
	protected void assertConnectorOperationIncrement(int expectedIncrementSmart, int expectedIncrementDumb) {
		assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, expectedIncrementDumb);
	}
	
	/**
	 * But "dumber" resource do not have any count simulation for groups.
	 */
	@Override
	protected Integer getExpectedLdapGroupCountTest25x() {
		return null;
	}

}
