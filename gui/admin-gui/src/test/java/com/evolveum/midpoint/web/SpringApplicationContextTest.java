/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.test.util.AbstractSpringTest;

/**
 * Test of spring application context initialization
 *
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "classpath:ctx-webapp.xml",
        "classpath:ctx-init.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-task.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-model.xml",
        "classpath*:ctx-cases.xml",
        "classpath*:ctx-workflow.xml" })
public class SpringApplicationContextTest extends AbstractSpringTest {

    @Autowired
    private ModelService modelService;
    @Autowired
    private InitialDataImport initialDataImport;

    @Test
    public void initApplicationContext() {
        assertNotNull(modelService);
        assertNotNull(initialDataImport);
    }
}
