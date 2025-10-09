/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl;

import static org.testng.AssertJUnit.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.test.util.AbstractSpringTest;

/**
 * Test of spring application context initialization
 *
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
public class SpringApplicationContextTest extends AbstractSpringTest {

    @Autowired
    RepositoryService repositoryService;

    @Test
    public void initApplicationContext() {
        assertNotNull(repositoryService);
    }
}
