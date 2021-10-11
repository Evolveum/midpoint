/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.validator.test;

import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;

@UnusedTestElement("3 tests failing, not in suite")
public class UnknownNodeSkipSchemaValidationTest extends UnknownNodeValidationTest {

    @Override
    protected void customizeValidator(LegacyValidator validator) {
        validator.setValidateSchema(false);
    }
}
