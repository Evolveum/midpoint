/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerListResourceObjectsTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerListResourceObjectsTest.class);
	@Autowired private ModelService controller;
	@Autowired @Qualifier("cacheRepositoryService") private RepositoryService repository;
	@Autowired private ProvisioningService provisioning;
	@Autowired private PrismContext prismContext;

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
		ObjectPaging paging = prismContext.queryFactory().createPaging(0, Integer.MAX_VALUE, (ItemPath) null, null);
		controller.listResourceObjects("1", new QName("local name"), paging, null, null);
	}
}
