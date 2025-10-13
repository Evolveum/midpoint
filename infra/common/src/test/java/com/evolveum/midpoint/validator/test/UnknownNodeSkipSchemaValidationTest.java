/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
