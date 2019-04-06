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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.AbstractGuiIntegrationTest;
import com.evolveum.midpoint.web.boot.MidPointSpringApplication;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * @author katka
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageUser extends AbstractGuiIntegrationTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestPageUser.class);
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		LOGGER.info("after super init");
		PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);
		
		LOGGER.info("adding system config page");
		addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
		
	}
	
	@Test
	public void test000renderPageUserNew() {
		
		LOGGER.info("render page user");
		PageParameters params = new PageParameters();
		params.add(OnePageParameterEncoder.PARAMETER, SystemObjectsType.USER_ADMINISTRATOR.value());
		PageUser pageUser = tester.startPage(PageUser.class);
		
		tester.assertRenderedPage(PageUser.class);
		
		//containers:0:container:values:0:value:propertiesLabel:properties:0:property:values:0:value:valueContainer:input:input
		String path = "tabPanel:panel:focusDetails:activation";
		
		tester.assertComponent(path, PrismContainerPanel.class);
	}
	
}
