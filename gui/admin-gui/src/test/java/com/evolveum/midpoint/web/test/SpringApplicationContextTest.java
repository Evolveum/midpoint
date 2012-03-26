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

package com.evolveum.midpoint.web.test;

import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Test of spring application context initialization
 *
 * @author Igor Farinic
 */

@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
        "file:src/main/webapp/WEB-INF/application-context-init.xml",
        "file:src/main/webapp/WEB-INF/application-context-security.xml",
        "classpath:application-context-test.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-task.xml",
        "classpath:application-context-audit.xml",
        "classpath:application-context-configuration-test.xml"})
//        // TODO: if these two contexts are initilized then the test fails for
//        // unknown reason
        //		"classpath:application-context-model.xml",
//        // "classpath:application-context-provisioning.xml",

public class SpringApplicationContextTest extends AbstractTestNGSpringContextTests {

    @Autowired(required = true)
    private ObjectTypeCatalog objectTypeCatalog;
    @Autowired(required = true)
    private ModelService modelService;
    @Autowired(required = true)
    private InitialDataImport initialDataImport;
    @Autowired(required = true)
    private RepositoryService repositoryService;

    @Test
    public void initApplicationContext() {
        assertNotNull(objectTypeCatalog.listSupportedObjectTypes());
        ObjectManager<GuiUserDto> objectManager = objectTypeCatalog.getObjectManager(UserType.class,
                GuiUserDto.class);
        UserManager userManager = (UserManager) (objectManager);
        assertNotNull(userManager);

        assertNotNull(modelService);
        assertNotNull(initialDataImport);
        assertNotNull(repositoryService);
    }
}
