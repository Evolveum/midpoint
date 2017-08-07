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
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Tests a slow semi manual resource with the use of proposed shadows.
 * The resource is "slow" in a way that it takes approx. a second to process a ticket.
 * This may cause all sorts of race conditions.
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualSlowProposed extends TestSemiManual {
		
	private static final Trace LOGGER = TraceManager.getTrace(TestSemiManualSlowProposed.class);
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		initManualConnector();
	}
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}
	
	@Override
	protected String getRoleOneOid() {
		return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}
	
	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}

	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID;
	}
	
	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE;
	}

	// Make the test fast ...
	@Override
	protected int getConcurrentTestRandomStartDelayRange() {
		return 300;
	}

	// ... and intense ...
	@Override
	protected int getConcurrentTestNumberOfThreads() {
		return 10;
	}
	
	// TODO: .. and make the resource slow.
	private void initManualConnector() {
		ManualConnectorInstance.setRandomDelayRange(1000);
	}

}