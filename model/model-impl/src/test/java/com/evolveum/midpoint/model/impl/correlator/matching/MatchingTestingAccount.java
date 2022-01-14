/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A {@link TestingAccount} customized to support ID Matcher tests.
 */
class MatchingTestingAccount extends TestingAccount {

    @Nullable String referenceId;
    @NotNull private final ExpectedMatchingResult expectedMatchingResult;

    MatchingTestingAccount(@NotNull PrismObject<ShadowType> account) {
        super(account);
        this.expectedMatchingResult = new ExpectedMatchingResult(getTestString());
    }

    @Nullable String getReferenceId() {
        return referenceId;
    }

    void setReferenceId(@Nullable String referenceId) {
        this.referenceId = referenceId;
    }

    @NotNull ExpectedMatchingResult getExpectedMatchingResult() {
        return expectedMatchingResult;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "account=" + shadow +
                ", referenceId='" + referenceId + '\'' +
                ", expectedMatchingResult=" + expectedMatchingResult +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "account", shadow, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "referenceId", referenceId, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "expectedMatchingResult",
                String.valueOf(expectedMatchingResult), indent + 1);
        return sb.toString();
    }
}
