/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertFalse;

import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.admin.resource.PageShadow;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageShadow extends AbstractInitializedGuiIntegrationTest {

    private static final String FORM_SAVE = "detailsView:mainForm:buttons:buttons:2";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test000testPageAccount() throws Exception {
        dummyResourceCtl.addAccount("test");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("test", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.debugComponentTrees("buttons");
        tester.assertComponent(FORM_SAVE, AjaxCompositedIconSubmitButton.class);
    }

    //TODO: enable after reviewed a know why shoud throw an error
    @Test (expectedExceptions = AssertionError.class, enabled = false)
    public void test001testPageAccountWithProtectedUser() throws Exception {
        dummyResourceCtl.addAccount("admin");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("admin", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.assertComponent(FORM_SAVE, AjaxCompositedIconSubmitButton.class);
    }

    @Test
    public void test010ActivationConfiguredForObjectTypeIsSupportedByHeader() {
        ResourceType resource = createResourceWithObjectTypeActivationCapability();
        ShadowType shadow = new ShadowType()
                .kind(ShadowKindType.ACCOUNT)
                .intent(SchemaConstants.INTENT_DEFAULT);

        assertFalse(ResourceTypeUtil.isActivationCapabilityEnabled(resource, null),
                "Activation must not be available at the resource level");
        assertFalse(ProvisioningObjectsUtil.activationNotSupported(resource, shadow),
                "Object-type configured activation capability should be supported");
    }

    private PageShadow renderPage(String userOid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, userOid);
        return renderPageWithParams(params);
    }

    private PageShadow renderPageWithParams(PageParameters params) {
        logger.info("render page account");
        if(params == null) {
            params = new PageParameters();
        }
        PageShadow pageAccount = tester.startPage(PageShadow.class, params);

        tester.assertRenderedPage(PageShadow.class);

        return pageAccount;
    }

    private ResourceType createResourceWithObjectTypeActivationCapability() {
        return new ResourceType()
                .schemaHandling(new SchemaHandlingType()
                        .objectType(new ResourceObjectTypeDefinitionType()
                                .kind(ShadowKindType.ACCOUNT)
                                .intent(SchemaConstants.INTENT_DEFAULT)
                                .configuredCapabilities(new CapabilityCollectionType()
                                        .activation(new ActivationCapabilityType()
                                                .enabled(true)
                                                .status(new ActivationStatusCapabilityType()
                                                        .enabled(true))))));
    }

}
