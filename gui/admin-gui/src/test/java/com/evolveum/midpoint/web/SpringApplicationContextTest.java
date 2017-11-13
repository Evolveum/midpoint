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

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.model.api.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

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
        "classpath*:ctx-workflow.xml"})
public class SpringApplicationContextTest extends AbstractTestNGSpringContextTests {

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
