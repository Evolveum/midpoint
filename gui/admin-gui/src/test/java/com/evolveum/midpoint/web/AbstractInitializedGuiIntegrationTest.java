/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import com.evolveum.midpoint.repo.api.RepoAddOptions;

import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public abstract class AbstractInitializedGuiIntegrationTest extends AbstractGuiIntegrationTest {

    protected static final String MAIN_FORM_OLD = "mainPanel:mainForm";
    protected static final String PATH_FORM_NAME_OLD = "tabPanel:panel:main:values:0:value:valueForm:valueContainer:input:"
            + "propertiesLabel:properties:0:property:values:0:value:valueForm:valueContainer:input:originValueContainer:"
            + "origValueWithButton:origValue:input";
    protected static final String FORM_SAVE_OLD = "save";

    protected static final String MAIN_FORM = "detailsView:mainForm";
    protected static final String PATH_FORM_NAME = "mainPanel:properties:container:1:valuesContainer:values:0:value:valueForm:valueContainer:"
            + "input:propertiesLabel:properties:0:property:valuesContainer:values:0:value:valueForm:valueContainer:input:originValueContainer:"
            + "origValueWithButton:origValue:input";
    protected static final String FORM_SAVE = "buttons:buttons:2:";

    protected DummyResource dummyResource;
    protected DummyResourceContoller dummyResourceCtl;
    protected ResourceType resourceDummyType;
    protected PrismObject<ResourceType> resourceDummy;

    protected PrismObject<UserType> userAdministrator;
    protected String accountJackOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        modelService.postInit(initResult);
        repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        userAdministrator = repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, initResult);
        login(userAdministrator);

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        resourceDummyType = resourceDummy.asObjectable();
        dummyResourceCtl.setResource(resourceDummy);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(USER_EMPTY_FILE, true, initResult);

        importObjectFromFile(ROLE_MAPMAKER_FILE);

        repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);
    }

    protected Page renderPage(Class<? extends Page> expectedRenderedPageClass) {
        return renderPage(expectedRenderedPageClass, null);
    }

    protected Page renderPage(Class<? extends Page> expectedRenderedPageClass, String oid) {
        logger.info("render page " + expectedRenderedPageClass.getSimpleName());
        PageParameters params = new PageParameters();
        if (oid != null) {
            params.add(OnePageParameterEncoder.PARAMETER, oid);
        }
        Page pageRole = tester.startPage(expectedRenderedPageClass, params);

        tester.assertRenderedPage(expectedRenderedPageClass);

        return pageRole;
    }

    protected void choiceArchetype(int order) {
        String tabPath = "detailsView:template:list:" + order + ":tile";
        tester.executeAjaxEvent(tabPath, "click");
    }
}
