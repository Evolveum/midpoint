/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.scripting;

/**
 * Tests legacy ("dynamic") versions of scripting expressions.
 */
public class TestScriptingBasicLegacy extends AbstractBasicScriptingTest {

    @Override
    String getSuffix() {
        return "-legacy";
    }

    @Override
    boolean isNew() {
        return false;
    }
}
