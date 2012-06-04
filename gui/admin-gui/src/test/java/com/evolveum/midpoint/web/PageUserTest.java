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

import com.evolveum.midpoint.web.page.admin.users.PageUser;
import org.apache.wicket.util.tester.WicketTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/application-context-webapp.xml",
        "file:src/main/webapp/WEB-INF/application-context-init.xml",
        "file:src/main/webapp/WEB-INF/application-context-security.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath*:application-context-repository.xml",
        "classpath:application-context-task.xml",
        "classpath:application-context-audit.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-common.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-model.xml",
        "classpath*:application-context-workflow.xml"})
public class PageUserTest extends AbstractTestNGSpringContextTests {

    @Test(enabled = false)
    public void testBasicRender() {
        WicketTester tester = new WicketTester();
        PageUser page = tester.startPage(PageUser.class);
        tester.assertRenderedPage(PageUser.class);
    }
}
