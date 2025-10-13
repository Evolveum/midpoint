/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.schema.TestConstants;

import java.io.File;

/**
 * @author Radovan Semancik
 * MID-5752
 */
public class TestParseShadowLegacy extends TestParseShadow {

    @Override
    protected File getFile() {
        return getFile(TestConstants.SHADOW_FILE_BASENAME_LEGACY);
    }

}
