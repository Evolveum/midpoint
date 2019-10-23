/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
