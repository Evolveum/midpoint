/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

/**
 * On native repository only.
 */
public class AbstractSimulationsTest extends AbstractEmptyModelIntegrationTest {

    static final File SIM_TEST_DIR = new File("src/test/resources/simulation");

    static final TestObject<MarkType> MARK_USER_ADD = TestObject.file(
            SIM_TEST_DIR, "mark-user-add.xml", "0c31f3a1-a7b1-4fad-8cea-eaafdc15daaf");
    private static final TestObject<MarkType> MARK_USER_DELETE = TestObject.file(
            SIM_TEST_DIR, "mark-user-delete.xml", "caa2921a-6cf4-4e70-ad2b-bfed278e29cf");
    private static final TestObject<MarkType> MARK_NONSENSE_MARK = TestObject.file(
            SIM_TEST_DIR, "mark-nonsense-mark.xml", "e2dccf40-9bfd-42a1-aa02-48b0f31cdb1c");

    private static final TestObject<RoleType> ROLE_PERSON = TestObject.file(
            SIM_TEST_DIR, "role-person.xml", "ba88cf08-06bc-470f-aeaa-511e86d5ea7f");
    private static final TestObject<RoleType> ROLE_PERSON_DEV = TestObject.file(
            SIM_TEST_DIR, "role-person-dev.xml", "5049daa8-5af8-4036-88af-5f374daf1340");
    private static final TestObject<RoleType> METAROLE = TestObject.file(
            SIM_TEST_DIR, "metarole.xml", "23c615ae-e0e0-4d81-86a0-712d7164b4d2");
    private static final TestObject<ObjectTemplateType> TEMPLATE_PERSON_INCLUDED_DEV = TestObject.file(
            SIM_TEST_DIR, "template-person-included-dev.xml", "3c27b909-5f79-4b24-a54f-85e7673f6782");
    private static final TestObject<ObjectTemplateType> TEMPLATE_PERSON = TestObject.file(
            SIM_TEST_DIR, "template-person.xml", "fec07d55-5bdd-4d9a-87f1-5f814303a4f5");
    private static final TestObject<ObjectTemplateType> TEMPLATE_PERSON_DEV_TEMPLATE = TestObject.file(
            SIM_TEST_DIR, "template-person-dev-template.xml", "a7f5bca6-4385-42ab-aca2-9740e1fa155f");
    static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            SIM_TEST_DIR, "archetype-person.xml", "f8d69091-02b3-436e-81fd-0f695f9045db");
    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON_DEV_ARCHETYPE = TestObject.file(
            SIM_TEST_DIR, "archetype-person-dev-archetype.xml", "be5bf6fb-11ce-40a8-b588-ec44cf051523");
    static final TestObject<ArchetypeType> ARCHETYPE_PERSON_DEV_TEMPLATE = TestObject.file(
            SIM_TEST_DIR, "archetype-person-dev-template.xml", "be7f8541-64ec-4bee-a5c3-855923ae9b90");
    static final TestObject<ArchetypeType> ARCHETYPE_CUSTOMER = TestObject.file(
            SIM_TEST_DIR, "archetype-customer.xml", "075ebbed-f3b9-4bac-90c2-bb8811121636");

    static final String ATTR_TYPE = "type";
    static final String ATTR_EMPLOYEE_NUMBER = "employeeNumber";
    private static final String ATTR_TELEPHONE_NUMBER = "telephoneNumber";
    private static final String ATTR_MAIL = "mail";

    static final ItemName ATTR_RI_TELEPHONE_NUMBER = new ItemName(NS_RI, ATTR_TELEPHONE_NUMBER);
    static final ItemName ATTR_RI_MAIL = new ItemName(NS_RI, ATTR_MAIL);

//    private static final ItemName ATTR_TYPE_ITEM_NAME = new ItemName(NS_RI, ATTR_TYPE_NAME);

    static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_TARGET = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-target.xml",
            "3f8d6dee-9663-496f-a718-b3c27234aca7",
            "simple-production-target",
            controller -> {
                controller.addAttrDef(
                        controller.getDummyResource().getAccountObjectClass(),
                        ATTR_TELEPHONE_NUMBER, String.class, false, false);
                controller.addAttrDef(
                        controller.getDummyResource().getAccountObjectClass(),
                        ATTR_MAIL, String.class, false, false);
            });
    static final DummyTestResource RESOURCE_SIMPLE_DEVELOPMENT_TARGET = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-development-target.xml",
            "572200ee-7499-47ec-9fdf-a575c96a5291",
            "simple-development-target");
    static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-source.xml",
            "c6caaa46-96c4-4244-883f-2771e18b82c9",
            "simple-production-source",
            controller -> addSourceAttributes(controller));

    static final DummyTestResource RESOURCE_SIMPLE_DEVELOPMENT_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-development-source.xml",
            "6d8ba4fd-95ee-4d98-80c2-3a194b566f89",
            "simple-development-source",
            controller -> addSourceAttributes(controller));

    private static void addSourceAttributes(DummyResourceContoller controller)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        controller.addAttrDef(
                controller.getDummyResource().getAccountObjectClass(),
                ATTR_TYPE, String.class, false, false);
        controller.addAttrDef(
                controller.getDummyResource().getAccountObjectClass(),
                ATTR_EMPLOYEE_NUMBER, String.class, false, false);
    }

    static final String METRIC_ATTRIBUTE_MODIFICATIONS_ID = "attribute-modifications";

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        if (!isNativeRepository()) {
            return; // No method will run anyway
        }

        CommonInitialObjects.addMarks(this, initTask, initResult);
        addObject(MARK_USER_ADD, initTask, initResult);
        addObject(MARK_USER_DELETE, initTask, initResult);
        addObject(MARK_NONSENSE_MARK, initTask, initResult);

        repoAdd(ROLE_PERSON, initResult);
        repoAdd(ROLE_PERSON_DEV, initResult);
        repoAdd(METAROLE, initResult);
        repoAdd(TEMPLATE_PERSON_INCLUDED_DEV, initResult);
        repoAdd(TEMPLATE_PERSON, initResult);
        repoAdd(TEMPLATE_PERSON_DEV_TEMPLATE, initResult);
        repoAdd(ARCHETYPE_PERSON, initResult);
        repoAdd(ARCHETYPE_PERSON_DEV_ARCHETYPE, initResult);
        repoAdd(ARCHETYPE_PERSON_DEV_TEMPLATE, initResult);
        repoAdd(ARCHETYPE_CUSTOMER, initResult);

        RESOURCE_SIMPLE_PRODUCTION_TARGET.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_DEVELOPMENT_TARGET.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_DEVELOPMENT_SOURCE.initAndTest(this, initTask, initResult);

        ARCHETYPE_REPORT.init(this, initTask, initResult);
        ARCHETYPE_COLLECTION_REPORT.init(this, initTask, initResult);
        REPORT_SIMULATION_VALUES_CHANGED.init(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return new File(SIM_TEST_DIR, "system-configuration.xml");
    }

    private ShadowType createAccount(DummyTestResource target) {
        return new ShadowType()
                .resourceRef(target.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default");
        // Name should be computed by mappings
    }

    ObjectReferenceType createLinkRefWithFullObject(DummyTestResource target) {
        return ObjectTypeUtil.createObjectRefWithFullObject(
                createAccount(target));
    }

    String addUser(String name, Task task, OperationResult result) throws CommonException {
        UserType user = new UserType()
                .name(name);

        var executed =
                executeChanges(user.asPrismObject().createAddDelta(), null, task, result);
        return ObjectDeltaOperation.findFocusDeltaOidInCollection(executed);
    }

    ObjectDelta<UserType> createLinkRefDelta(String userOid, DummyTestResource target) throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(createLinkRefWithFullObject(target))
                .asObjectDelta(userOid);
    }

    ObjectDelta<UserType> createAssignmentDelta(String userOid, DummyTestResource target) throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(createAssignmentValue(target))
                .asObjectDelta(userOid);
    }

    static AssignmentType createAssignmentValue(DummyTestResource target) {
        return new AssignmentType()
                .construction(
                        new ConstructionType()
                                .resourceRef(target.oid, ResourceType.COMPLEX_TYPE));
    }
}
