/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Testing the shadow marks functionality.
 *
 * Basic tests (adding, modifying, deleting, syncing protected shadow) are already covered by standard TestDummy* tests.
 *
 * This is *not* a complete dummy test: only a few methods are contained here.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyShadowMarks extends AbstractDummyTest {

    private static final File RESOURCE_DUMMY_SHADOW_MARKS_FILE = new File(TEST_DIR,"resource-dummy-shadow-marks.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_SHADOW_MARKS_FILE;
    }

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        assertSuccess(
                provisioningService.testResource(RESOURCE_DUMMY_OID, initTask, initResult));

        resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, initResult);
        resourceBean = resource.asObjectable();
    }

    @Test
    public void test100MarkAndUnmarkShadow() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("a non-protected account on the resource");
        var shadowOid = provisioningService.addObject(
                createAccountShadow(accountName),
                null,
                null,
                task,
                result);

        var shadowBefore = provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(shadowBefore, "before")
                .assertNotProtected();

        when("marking the shadow as protected (via statement)");
        markShadow(shadowOid, MARK_PROTECTED_OID, task, result);

        then("shadow is marked as protected (when obtained by provisioning)");
        var accountAfter = provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(accountAfter, "after")
                .assertProtected()
                .assertEffectiveMarks(MARK_PROTECTED_OID);

        var accountAfterRepo = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        assertShadow(accountAfterRepo, "after")
                .assertEffectiveMarks(MARK_PROTECTED_OID);

        when("shadow is unmarked");
        unmarkShadow(shadowOid, MARK_PROTECTED_OID, task, result);

        then("shadow is no longer marked as protected (when obtained by provisioning)");
        var accountAfter2 = provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(accountAfter2, "after")
                .assertNotProtected()
                .assertEffectiveMarks();

        var accountAfterRepo2 = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        assertShadow(accountAfterRepo2, "after")
                .assertEffectiveMarks();
    }

    @Test
    public void test110MarkOnClassification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var groupName = getTestNameShort();

        given("a group on the resource");
        dummyResource.addGroup(new DummyGroup(groupName));

        when("group is retrieved from the resource");
        var shadows = provisioningService.searchShadows(
                Resource.of(resource)
                        .queryFor(RI_GROUP_OBJECT_CLASS)
                        .and().item(ICFS_NAME_PATH).eq(groupName)
                        .build(),
                null, task, result);
        assertThat(shadows).as("matching shadows").hasSize(1);
        var shadow = shadows.get(0);

        then("shadow is correctly marked");
        assertShadowAfter(shadow.getPrismObject())
                .assertEffectiveMarks(MARK_INVALID_DATA.oid);
    }

    private PrismObject<ShadowType> createAccountShadow(String username) throws SchemaException, ConfigurationException {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        ShadowType shadowType = new ShadowType();
        shadowType.setName(PrismTestUtil.createPolyStringType(username));
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        shadowType.setResourceRef(resourceRef);
        shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
        PrismObject<ShadowType> shadow = shadowType.asPrismObject();
        PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(SchemaConstants.ICFS_NAME);
        icfsNameProp.setRealValue(username);
        return shadow;
    }
}
