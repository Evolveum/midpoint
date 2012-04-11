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
package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.web.controller.AboutController.SystemItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/application-context-webapp.xml",
        "file:src/main/webapp/WEB-INF/application-context-init.xml",
        "file:src/main/webapp/WEB-INF/application-context-security.xml",
        "classpath:application-context-test.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath*:application-context-repository.xml",
        "classpath:application-context-task.xml",
        "classpath:application-context-audit.xml",
        "classpath:application-context-configuration-test.xml"})
public class AboutControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired(required = true)
    AboutController controller;

    @BeforeMethod
    public void before() {
        AssertJUnit.assertNotNull(controller);
    }

    @Test
    public void getItems() {
        List<SystemItem> items = controller.getItems();
        AssertJUnit.assertNotNull(items);
        AssertJUnit.assertEquals(11, items.size());

        for (SystemItem item : items) {
            AssertJUnit.assertNotNull(item);
            assertNotNull(item.getProperty());
            assertNotNull(item.getValue());

            AssertJUnit.assertFalse("".equals(item.getProperty()));
            AssertJUnit.assertFalse("".equals(item.getValue()));
        }
    }
}
