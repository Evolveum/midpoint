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
package com.evolveum.midpoint.model.impl.controller;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerListResourceObjectsTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerListResourceObjectsTest.class);
	@Autowired(required = true)
	private ModelService controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResourceOid() throws Exception {
		controller.listResourceObjects(null, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyResourceOid() throws Exception {
		controller.listResourceObjects("", null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullQName() throws Exception {
		controller.listResourceObjects("1", null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullPaging() throws Exception {
		controller.listResourceObjects("1", new QName("local name"), null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		ObjectPaging paging = ObjectPaging.createPaging(0, Integer.MAX_VALUE, ObjectType.F_NAME, OrderDirection.ASCENDING);
		controller.listResourceObjects("1", new QName("local name"), paging, null, null);
	}
}
