/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.match;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.model.impl.correlator.AbstractCorrelatorOrMatcherTest.ATTR_TEST_QNAME;

public class TestingAccount implements DebugDumpable {

    @NotNull private final ShadowType account;
    @Nullable String referenceId;
    @NotNull private final ExpectedMatchingResult expectedMatchingResult;

    public TestingAccount(@NotNull PrismObject<ShadowType> account) {
        this.account = account.asObjectable();
        String testString;
        try {
            testString = ShadowUtil.getAttributeValue(account, ATTR_TEST_QNAME);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        this.expectedMatchingResult = new ExpectedMatchingResult(testString);
    }

    public int getNumber() {
        return Integer.parseInt(account.getName().getOrig());
    }

    public @NotNull ShadowAttributesType getAttributes() {
        return account.getAttributes();
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
        return "TestingAccount{" +
                "account=" + account +
                ", referenceId='" + referenceId + '\'' +
                ", expectedMatchingResult=" + expectedMatchingResult +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "account", account, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "referenceId", referenceId, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "expectedMatchingResult",
                String.valueOf(expectedMatchingResult), indent + 1);
        return sb.toString();
    }
}
