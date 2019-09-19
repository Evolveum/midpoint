/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import com.evolveum.midpoint.web.component.objectdetails.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
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
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.AdminGuiTestConstants;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
//@ImportResource(locations = {
//		"classpath:ctx-init.xml"
//})
public class TestPageAccount extends AbstractInitializedGuiIntegrationTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestPageAccount.class);
	
	private static final String FORM_SAVE = "mainForm:save";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);
		
		LOGGER.info("adding system config page");
		addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
	}
	
	@Test
	public void test000testPageAccount() throws Exception {
		dummyResourceCtl.addAccount("test");
		
		PrismObject<ShadowType> accountMancomb = findAccountByUsername("test", dummyResourceCtl.getResource());
		renderPage(accountMancomb.getOid());
		tester.assertComponent(FORM_SAVE, AjaxSubmitButton.class);
	}
	
	@Test (expectedExceptions = AssertionError.class)
	public void test001testPageAccountWithProtectedUser() throws Exception {
		dummyResourceCtl.addAccount("admin");
		
		PrismObject<ShadowType> accountMancomb = findAccountByUsername("admin", dummyResourceCtl.getResource());
		renderPage(accountMancomb.getOid());
		tester.assertComponent(FORM_SAVE, AjaxSubmitButton.class);
	}
	
	private PageAccount renderPage(String userOid) {
		PageParameters params = new PageParameters();
		params.add(OnePageParameterEncoder.PARAMETER, userOid);
		return renderPageWithParams(params);
	}
	
	private PageAccount renderPageWithParams(PageParameters params) {
		LOGGER.info("render page user");
		if(params == null) {
			params = new PageParameters();
		}
		PageAccount pageAccount = tester.startPage(PageAccount.class, params);
		
		tester.assertRenderedPage(PageAccount.class);
		
		return pageAccount;
	}
	
}
