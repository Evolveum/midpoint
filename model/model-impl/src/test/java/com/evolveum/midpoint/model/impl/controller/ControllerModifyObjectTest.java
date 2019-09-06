/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerModifyObjectTest extends AbstractTestNGSpringContextTests {

	//private static final Trace LOGGER = TraceManager.getTrace(ControllerModifyObjectTest.class);
	@Autowired
	private ModelCrudService controller;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired
	private ProvisioningService provisioning;
	@Autowired
	private TaskManager taskManager;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChange() throws Exception {
		controller.modifyObject(UserType.class, null, null, null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChangeOid() throws Exception {
		controller.modifyObject(UserType.class, null, new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyChangeOid() throws Exception {
		controller.modifyObject(UserType.class, "", new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.modifyObject(UserType.class, "1", new ArrayList<>(), null, taskManager.createTaskInstance(), null);
	}
}
