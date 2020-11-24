/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import static java.util.Collections.singletonList;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class TestAsyncUpdateCaching extends TestAsyncUpdate {

    static final File RESOURCE_ASYNC_CACHING_FILE = new File(TEST_DIR, "resource-async-caching.xml");

    @Override
    protected File getResourceFile() {
        return RESOURCE_ASYNC_CACHING_FILE;
    }

    @NotNull
    @Override
    public List<String> getConnectorTypes() {
        return singletonList(ASYNC_UPDATE_CONNECTOR);
    }

    @Override
    boolean isCached() {
        return true;
    }
}
