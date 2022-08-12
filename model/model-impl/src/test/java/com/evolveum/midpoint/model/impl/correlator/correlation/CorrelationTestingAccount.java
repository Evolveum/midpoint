/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.*;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

/**
 * A {@link TestingAccount} customized to support correlators tests.
 */
public class CorrelationTestingAccount extends TestingAccount {

    private static final String CANDIDATES_START = "#";
    private static final String CANDIDATES_SEPARATOR = ";";
    private static final String CANDIDATE_CONFIDENCE_SEPARATOR = ":";

    private static final String NONE = "_none";
    private static final String UNCERTAIN = "_uncertain";

    public static final String ATTR_EXP_CANDIDATES = "expCandidates"; // used for the test itself
    private static final ItemName ATTR_EXP_CANDIDATES_QNAME = new ItemName(NS_RI, ATTR_EXP_CANDIDATES);
    public static final String ATTR_EXP_RESULT = "expResult"; // used for the test itself
    private static final ItemName ATTR_EXP_RESULT_QNAME = new ItemName(NS_RI, ATTR_EXP_RESULT);

    CorrelationTestingAccount(@NotNull PrismObject<ShadowType> account) {
        super(account);
    }

    private String getAttrExpCandidates() {
        try {
            return ShadowUtil.getAttributeValue(shadow, ATTR_EXP_CANDIDATES_QNAME);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    private String getAttrExpResult() {
        try {
            return ShadowUtil.getAttributeValue(shadow, ATTR_EXP_RESULT_QNAME);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    CorrelationSituationType getExpectedCorrelationSituation() {
        String stringValue = getAttrExpResult();
        if (stringValue == null || stringValue.isEmpty()) {
            throw new IllegalStateException("Invalid expected result ('expResult' attribute): '" + stringValue + "'");
        } else if (stringValue.startsWith(NONE)) {
            return NO_OWNER;
        } else if (stringValue.startsWith(UNCERTAIN)) {
            return CorrelationSituationType.UNCERTAIN;
        } else {
            return EXISTING_OWNER;
        }
    }

    String getExpectedOwnerName() {
        if (getExpectedCorrelationSituation() == EXISTING_OWNER) {
            return StringUtils.substringBefore(getAttrExpResult(), CANDIDATES_START);
        } else {
            return null;
        }
    }

    @NotNull Set<TestCandidateOwner> getCandidateOwners(boolean complete) {
        String expected;
        if (complete) {
            String expResult = emptyIfNull(getAttrExpResult()).trim();
            int i = expResult.indexOf(CANDIDATES_START);
            if (i < 0) {
                // Default is to check from expCandidates
                expected = emptyIfNull(getAttrExpCandidates()).trim();
            } else {
                expected = expResult.substring(i + 1);
            }
        } else {
            expected = emptyIfNull(getAttrExpCandidates()).trim();
        }
        if ("".equals(expected)) {
            return Set.of();
        }
        Set<TestCandidateOwner> candidateOwnerSet = new HashSet<>();
        String[] candidatesAndConfidences = expected.split(CANDIDATES_SEPARATOR);
        for (String candidateAndConfidence : candidatesAndConfidences) {
            String[] parts = candidateAndConfidence.split(CANDIDATE_CONFIDENCE_SEPARATOR);
            TestCandidateOwner candidateOwner;
            if (parts.length == 1) {
                candidateOwner = new TestCandidateOwner(parts[0], 1.0);
            } else if (parts.length == 2) {
                candidateOwner = new TestCandidateOwner(parts[0], Double.parseDouble(parts[1]));
            } else {
                throw new IllegalStateException("Wrong candidate-confidence pair: '" + candidateAndConfidence + "' in " + this);
            }
            candidateOwnerSet.add(candidateOwner);
        }
        return candidateOwnerSet;
    }
}
