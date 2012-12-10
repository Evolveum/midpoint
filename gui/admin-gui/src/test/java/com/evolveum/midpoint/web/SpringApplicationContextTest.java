/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/ctx-webapp.xml",
        "file:src/main/webapp/WEB-INF/ctx-init.xml",
        "file:src/main/webapp/WEB-INF/ctx-security.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath*:ctx-repository.xml",
        "classpath:ctx-task.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-model.xml",
        "classpath*:ctx-workflow.xml"})
public class SpringApplicationContextTest extends AbstractTestNGSpringContextTests {

    @Autowired(required = true)
    private ModelService modelService;
    @Autowired(required = true)
    private InitialDataImport initialDataImport;

    @Test
    public void initApplicationContext() {
        assertNotNull(modelService);
        assertNotNull(initialDataImport);
    }
}
