/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.File;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

public class AbstractSimulationsTest extends AbstractEmptyModelIntegrationTest {

    private static final File SIM_TEST_DIR = new File("src/test/resources/simulation");

    static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_TARGET = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-target.xml",
            "3f8d6dee-9663-496f-a718-b3c27234aca7",
            "simple-production-target");
    static final DummyTestResource RESOURCE_SIMPLE_DEVELOPMENT_TARGET = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-development-target.xml",
            "572200ee-7499-47ec-9fdf-a575c96a5291",
            "simple-development-target");
    static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-source.xml",
            "c6caaa46-96c4-4244-883f-2771e18b82c9",
            "simple-production-source");
    private static final DummyTestResource RESOURCE_SIMPLE_DEVELOPMENT_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-development-source.xml",
            "6d8ba4fd-95ee-4d98-80c2-3a194b566f89",
            "simple-development-source");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SIMPLE_PRODUCTION_TARGET.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_DEVELOPMENT_TARGET.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_DEVELOPMENT_SOURCE.initAndTest(this, initTask, initResult);
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
                .add(new AssignmentType()
                        .construction(
                                new ConstructionType()
                                        .resourceRef(target.oid, ResourceType.COMPLEX_TYPE)))
                .asObjectDelta(userOid);
    }
}
