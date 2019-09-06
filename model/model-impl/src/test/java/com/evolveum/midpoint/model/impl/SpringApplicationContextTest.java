/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.repo.api.RepositoryService;

/**
 *
 * Test of spring application context initialization
 *
 * @author Igor Farinic
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
public class SpringApplicationContextTest extends AbstractTestNGSpringContextTests  {

	@Autowired(required = true)
	RepositoryService repositoryService;

	@Test
	public void initApplicationContext() {
		assertNotNull(repositoryService);
	}
}
