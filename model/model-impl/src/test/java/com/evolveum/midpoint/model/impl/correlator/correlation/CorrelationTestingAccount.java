/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A {@link TestingAccount} customized to support correlators tests.
 */
class CorrelationTestingAccount extends TestingAccount {

    private static final String NONE = "_none";
    private static final String UNCERTAIN = "_uncertain";

    CorrelationTestingAccount(@NotNull PrismObject<ShadowType> account) {
        super(account);
    }

    CorrelationResult.Status getExpectedCorrelationStatus() {
        String testString = getTestString();
        if (testString == null || testString.isEmpty()) {
            throw new IllegalStateException("Invalid expected result ('test' attribute): '" + testString + "'");
        }
        if (NONE.equals(testString)) {
            return CorrelationResult.Status.NO_OWNER;
        } else if (UNCERTAIN.equals(testString)) {
            return CorrelationResult.Status.UNCERTAIN;
        } else {
            return CorrelationResult.Status.EXISTING_OWNER;
        }
    }

    String getExpectedOwnerName() {
        if (getExpectedCorrelationStatus() == CorrelationResult.Status.EXISTING_OWNER) {
            return getTestString();
        } else {
            return null;
        }
    }
}
