/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import org.testng.annotations.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.model.impl.sync.Action;
import com.evolveum.midpoint.model.impl.sync.ActionManager;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ActionManagerImplTest extends AbstractTestNGSpringContextTests  {

	@Autowired(required = true)
	private ActionManager<? extends Action> manager;

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void setActionMappingNull() {
		manager.setActionMapping(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getActionInstanceNullUri() {
		manager.getActionInstance(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getctionInstanceEmptyUri() {
		manager.getActionInstance("");
	}

	@Test
	public void getActionInstanceNullParameters() {
		Action filter = manager
				.getActionInstance("http://midpoint.evolveum.com/xml/ns/public/model/action-3#addUser");
		assertNotNull(filter);
	}

	@Test
	public void getActionInstanceNotExistingFilter() {
		Action filter = manager.getActionInstance("notExistingUri");
		assertNull(filter);
	}
}
