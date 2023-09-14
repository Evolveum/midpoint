/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

public class TestAsyncUpdateNoCaching extends TestAsyncUpdate {

    private static final File RESOURCE_ASYNC_NO_CACHING_FILE = new File(TEST_DIR, "resource-async-no-caching.xml");

    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_NO_CACHING_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        dummyResourceCtl = DummyResourceContoller.create("async");
        dummyResourceCtl.setResource(resource);
        dummyResource = dummyResourceCtl.getDummyResource();
        dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(), "test", String.class, false, true);
    }

    @NotNull
    @Override
    public List<String> getConnectorTypes() {
        return Arrays.asList(ASYNC_UPDATE_CONNECTOR, IntegrationTestTools.DUMMY_CONNECTOR_TYPE);
    }

    @Override
    boolean isCached() {
        return false;
    }

    @Override
    protected void addDummyAccount(String name) {
        try {
            dummyResourceCtl.addAccount(name);
        } catch (ObjectAlreadyExistsException | SchemaViolationException | ConnectException | FileNotFoundException |
                 ConflictException | InterruptedException | ObjectDoesNotExistException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void setDummyAccountTestAttribute(String name, String... values) {
        try {
            DummyAccount account = dummyResource.getAccountByUsername(name);
            account.replaceAttributeValues("test", Arrays.asList(values));
        } catch (SchemaViolationException | ConnectException | FileNotFoundException | ConflictException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected boolean hasReadCapability() {
        return true;
    }
}
