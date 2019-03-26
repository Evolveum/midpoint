/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui;

import javax.annotation.security.RunAs;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;


import com.evolveum.midpoint.web.AbstractGuiIntegrationTest;
import com.evolveum.midpoint.web.boot.MidPointSpringApplication;
import com.evolveum.midpoint.web.page.admin.users.PageUser;

/**
 * @author katka
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = MidPointSpringApplication.class)
public class TestPageUser extends AbstractGuiIntegrationTest {

	
	@Test
	public void test000renderPageUserNew() {
		
		PageUser pageUser = tester.startPage(PageUser.class);
		
		tester.assertRenderedPage(PageUser.class);
	}
	
}
