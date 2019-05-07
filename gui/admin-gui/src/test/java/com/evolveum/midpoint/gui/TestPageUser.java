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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValuePanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.AbstractGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author katka
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageUser extends AbstractGuiIntegrationTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestPageUser.class);
	
	private static final String TAB_MAIN = "mainPanel:mainForm:tabPanel:panel:main";
	private static final String TAB_ACTIVATION = "mainPanel:mainForm:tabPanel:panel:activation";
	private static final String TAB_PASSWORD = "mainPanel:mainForm:tabPanel:panel:password";
	private static final String MAIN_FORM = "mainPanel:mainForm";
	
	private static final String PATH_FORM_NAME = "tabPanel:panel:main:value:propertiesLabel:properties:0:property:values:0:valueContainer:form:input:input";
	private static final String PATH_FORM_ADMINISTRATIVE_STATUS = "tabPanel:panel:activation:values:0:value:propertiesLabel:properties:0:property:values:0:valueContainer:form:input:input";
	private static final String PATH_PASSWORD_NEW = "tabPanel:panel:password:values:0:value:propertiesLabel:properties:1:property:values:0:valueContainer:form:input:inputContainer:password1";
	private static final String PATH_PASSWORD_NEW_REPEAT = "tabPanel:panel:password:values:0:value:propertiesLabel:properties:1:property:values:0:valueContainer:form:input:inputContainer:password2";
	private static final String FORM_SAVE = "save";
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		LOGGER.info("after super init");
		PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);
		
		LOGGER.info("adding system config page");
		addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
		
	}
	
	@Test
	public void test000testPageUserNew() {
		renderPage();
	}
	
	@Test
	public void test001testBasicTab() {
		PageUser pageUser = renderPage();
		
		tester.assertComponent(TAB_MAIN, PrismObjectValuePanel.class);
		
		tester.assertComponent(TAB_ACTIVATION, PrismContainerPanel.class);
		
		tester.assertComponent(TAB_PASSWORD, PrismContainerPanel.class);
		
	}
	
	@Test
	public void test002testAddDelta() throws Exception {
		PageUser pageUser = renderPage();
		
		FormTester formTester = tester.newFormTester(MAIN_FORM);
		formTester.setValue(PATH_FORM_NAME, "newUser");
		formTester.setValue(PATH_FORM_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED.value());
		formTester.setValue(PATH_PASSWORD_NEW, "n3wP4ss");
		formTester.setValue(PATH_PASSWORD_NEW_REPEAT, "n3wP4ss");
				
		formTester = formTester.submit(FORM_SAVE);
		
//		assertInfoMessages("Save successfull");
//		
//		PrismObject<UserType> newUser = findObjectByName(UserType.class, "newUser");
//		LOGGER.info("created user: {}", newUser.debugDump());
		
	}
	
	@Test
	public void test010renderAssignmentsTab() {
		
		renderPage();
		
		tester.assertRenderedPage(PageUser.class);
		
		String assignmentPath = "mainPanel:mainForm:tabPanel:tabs-container:tabs:2:link";
		tester.clickLink(assignmentPath);
		
		//TODO assignments table
//		String assignmentTable = assignmentPath + ":table";
//		
//		tester.assertComponent(assignmentTable, MultivalueContainerListPanelWithDetailsPanel.class);
	}
	
	private PageUser renderPage() {
		LOGGER.info("render page user");
		PageParameters params = new PageParameters();
		PageUser pageUser = tester.startPage(PageUser.class);
		
		tester.assertRenderedPage(PageUser.class);
		
		return pageUser;
		
	}
	
}
