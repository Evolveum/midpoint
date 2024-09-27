/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_GROUP_OBJECT_CLASS;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests various scenarios involving multiple object types (intents) as well as multiple association types.
 *
 * See also `TestIntent` in `model-intest` module.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyIntents extends AbstractDummyTest {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-intents");

    private static final ItemName RI_ORG = ItemName.from(SchemaConstants.NS_RI, "org");
    private static final ItemName RI_INTERNAL_GROUP = ItemName.from(SchemaConstants.NS_RI, "internalGroup");
    private static final ItemName RI_EXTERNAL_GROUP = ItemName.from(SchemaConstants.NS_RI, "externalGroup");

    private static final DummyTestResource RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT = new DummyTestResource(
            TEST_DIR, "resource-no-default-account.xml", "ae8a1129-1e81-4097-a1cf-6dacfc03e5ff",
            "no-default-account");
    private static final DummyTestResource RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED = new DummyTestResource(
            TEST_DIR, "resource-no-default-account-fixed.xml", "f75a77ad-db18-445f-a477-30fac55ec1ef",
            "no-default-account-fixed");
    private static final DummyTestResource RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT = new DummyTestResource(
            TEST_DIR, "resource-with-default-account.xml", "744fadc5-d868-485c-b452-0d1727c5c1eb",
            "with-default-account");
    private static final DummyTestResource RESOURCE_DUMMY_ASSOCIATIONS = new DummyTestResource(
            TEST_DIR, "resource-associations.xml", "a19a5fc7-b78d-497c-b5d9-0d388f140f22",
            "associations", c -> {
        c.populateWithDefaultSchema();
        c.addAttrDef(c.getDummyResource().getAccountObjectClass(),
                "organizations", String.class, false, false);
    });

    private static final String INTENT_MAIN = "main";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initResource(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT, initTask, initResult);
        initResource(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED, initTask, initResult);
        initResource(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT, initTask, initResult);

        RESOURCE_DUMMY_ASSOCIATIONS.initAndTest(this, initTask, initResult);
    }

    private void initResource(DummyTestResource resource, Task initTask, OperationResult initResult) throws Exception {
        initDummyResource(resource, initResult);
        testResourceAssertSuccess(resource, initTask, initResult);

        var alice = resource.controller.addAccount("alice");
        var wheel = resource.controller.addGroup("wheel");
        var create = new DummyPrivilege("create");
        resource.controller.getDummyResource().addPrivilege(create);

        wheel.addMember(alice.getName());
        alice.addAttributeValue(DummyAccount.ATTR_PRIVILEGES_NAME, create.getName());
    }

    @Test
    public void test100RetrievingAccountsByObjectClass() throws Exception {
        deleteAllShadows();

        // associations are not part of the OC definition, so they are not fetched
        executeSearchByObjectClass(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT, false);

        // here they are part of the object class definition
        executeSearchByObjectClass(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED, true);
        executeSearchByObjectClass(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT, true);
    }

    @Test
    public void test110RetrievingAccountsByKindAndIntent() throws Exception {
        deleteAllShadows();

        executeSearchByKindAndIntent(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT);
        executeSearchByKindAndIntent(RESOURCE_DUMMY_NO_DEFAULT_ACCOUNT_FIXED);
        executeSearchByKindAndIntent(RESOURCE_DUMMY_WITH_DEFAULT_ACCOUNT);
    }

    private void executeSearchByObjectClass(DummyTestResource resource, boolean associationsVisible) throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("shadows are retrieved by object class");
        var shadows = provisioningService.searchShadows(
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        assertThat(shadows).hasSize(1);
        var shadow = shadows.iterator().next().getBean();

        if (associationsVisible) {
            then("shadow contains the associations");
            assertAssociationsPresent(shadow);
        } else {
            then("shadow does not contain the associations");
            assertAssociationsNotPresent(shadow);
        }
    }

    private void executeSearchByKindAndIntent(DummyTestResource resource) throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("shadows are retrieved by kind and intent");
        var shadows = provisioningService.searchShadows(
                Resource.of(resource.get())
                        .queryFor(ShadowKindType.ACCOUNT, INTENT_MAIN)
                        .build(),
                null, task, result);

        assertThat(shadows).hasSize(1);
        var shadow = shadows.iterator().next().getBean();

        then("shadow contains the associations");
        assertAssociationsPresent(shadow);
    }

    private void assertAssociationsPresent(ShadowType shadow) {
        assertShadow(shadow, "")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_MAIN)
                .associations()
                .association(RI_GROUP)
                .assertSize(1)
                .end()
                .association(RI_PRIV)
                .assertSize(1)
                .end();
    }

    private void assertAssociationsNotPresent(ShadowType shadow) {
        assertShadow(shadow, "")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_MAIN)
                .associations()
                .assertValuesCount(0)
                .end();
    }

    private void deleteAllShadows() throws CommonException {
        var result = getTestOperationResult();
        var shadows = repositoryService.searchObjects(ShadowType.class, null, null, result);
        for (var shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }

    /** Tests `createTargetObjectsFilter` methods; MID-10023. */
    @Test
    public void test200TargetObjectFilters() throws Exception {
        var schema = Resource.of(RESOURCE_DUMMY_ASSOCIATIONS.get()).getCompleteSchemaRequired();
        var accountDef = schema.getObjectTypeDefinitionRequired(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT);
        var groupDef = schema.findObjectClassDefinitionRequired(RI_GROUP_OBJECT_CLASS);
        var privDef = schema.findObjectClassDefinitionRequired(RI_CUSTOM_PRIVILEGE_OBJECT_CLASS);
        var orgDef = schema.findObjectClassDefinitionRequired(RI_CUSTOM_ORG_OBJECT_CLASS);

        var prefix = "resourceRef matches (oid = '" + RESOURCE_DUMMY_ASSOCIATIONS.oid + "' and targetType = ResourceType)";

        // Filters for reference attributes refer only to the resource object and the object class.

        assertShadowFilter(
                "'group' ref attr target filter (exact)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_GROUP)
                        .createTargetObjectsFilter(false),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass'");

        assertShadowFilter(
                "'group' ref attr target filter (resource-safe)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_GROUP)
                        .createTargetObjectsFilter(true),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass'");

        assertShadowFilter(
                "'priv' ref attr target filter (exact)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_PRIV)
                        .createTargetObjectsFilter(false),
                privDef,
                prefix + " and objectClass = 'ri:CustomprivilegeObjectClass'");

        assertShadowFilter(
                "'priv' ref attr target filter (resource-safe)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_PRIV)
                        .createTargetObjectsFilter(true),
                privDef,
                prefix + " and objectClass = 'ri:CustomprivilegeObjectClass'");

        assertShadowFilter(
                "'org' ref attr target filter (exact)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_ORG)
                        .createTargetObjectsFilter(false),
                orgDef,
                prefix + " and objectClass = 'ri:CustomorgObjectClass'");

        assertShadowFilter(
                "'org' ref attr target filter (resource-safe)",
                accountDef
                        .findReferenceAttributeDefinitionRequired(RI_ORG)
                        .createTargetObjectsFilter(true),
                orgDef,
                prefix + " and objectClass = 'ri:CustomorgObjectClass'");

        // Filters for associations may refer to kinds and intents as well.

        assertShadowFilter(
                "'internalGroup' association target filter (exact)",
                accountDef
                        .findAssociationDefinitionRequired(RI_INTERNAL_GROUP)
                        .createTargetObjectsFilter(false),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass' "
                        + "and ((kind = 'entitlement' and intent = 'application-group') "
                        + "or (kind = 'entitlement' and intent = 'basic-group'))");

        assertShadowFilter(
                "'internalGroup' association target filter (resource-safe)",
                accountDef
                        .findAssociationDefinitionRequired(RI_INTERNAL_GROUP)
                        .createTargetObjectsFilter(true),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass'");

        assertShadowFilter(
                "'externalGroup' association target filter (exact)",
                accountDef
                        .findAssociationDefinitionRequired(RI_EXTERNAL_GROUP)
                        .createTargetObjectsFilter(false),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass' "
                        + "and kind = 'entitlement' and intent = 'external-group'");

        assertShadowFilter(
                "'externalGroup' association target filter (resource-safe)",
                accountDef
                        .findAssociationDefinitionRequired(RI_EXTERNAL_GROUP)
                        .createTargetObjectsFilter(true),
                groupDef,
                prefix + " and objectClass = 'ri:GroupObjectClass' "
                        + "and kind = 'entitlement' and intent = 'external-group'");

        assertShadowFilter(
                "'priv' association target filter (exact)",
                accountDef
                        .findAssociationDefinitionRequired(RI_PRIV)
                        .createTargetObjectsFilter(false),
                privDef,
                prefix + " and objectClass = 'ri:CustomprivilegeObjectClass'");

        assertShadowFilter(
                "'priv' association target filter (resource-safe)",
                accountDef
                        .findAssociationDefinitionRequired(RI_PRIV)
                        .createTargetObjectsFilter(true),
                privDef,
                prefix + " and objectClass = 'ri:CustomprivilegeObjectClass'");
    }

    /** Checking the schema for simple association values. */
    @Test
    public void test210AssociationSchema() throws CommonException {
        var assocDef = Resource.of(RESOURCE_DUMMY_ASSOCIATIONS.get())
                .getCompleteSchemaRequired()
                .getObjectTypeDefinitionRequired(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT)
                .findAssociationDefinitionRequired(RI_INTERNAL_GROUP);

        displayDumpable("association definition", assocDef);
        displayDumpable("association value CTD", assocDef.getComplexTypeDefinition());
        displayValue("participants", assocDef.getObjectParticipantNames());

        assertThat(assocDef.getObjectParticipantNames())
                .as("object participant names")
                .containsExactly(RI_GROUP);
    }
}
