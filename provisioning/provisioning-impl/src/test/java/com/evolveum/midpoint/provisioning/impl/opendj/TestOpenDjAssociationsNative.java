/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

public class TestOpenDjAssociationsNative extends AbstractOpenDjAssociationsTest {

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-native.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_FILE;
    }
}
