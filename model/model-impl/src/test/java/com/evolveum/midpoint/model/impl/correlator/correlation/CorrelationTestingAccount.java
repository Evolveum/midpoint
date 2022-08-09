/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

/**
 * A {@link TestingAccount} customized to support correlators tests.
 */
class CorrelationTestingAccount extends TestingAccount {

    private static final String CANDIDATES_START = "#";
    private static final String CANDIDATES_SEPARATOR = ";";
    private static final String CANDIDATE_CONFIDENCE_SEPARATOR = ":";

    private static final String NONE = "_none";
    private static final String UNCERTAIN = "_uncertain";

    CorrelationTestingAccount(@NotNull PrismObject<ShadowType> account) {
        super(account);
    }

    CorrelationSituationType getExpectedCorrelationSituation() {
        String testString = getTestString();
        if (testString == null || testString.isEmpty()) {
            throw new IllegalStateException("Invalid expected result ('test' attribute): '" + testString + "'");
        } else if (testString.startsWith(NONE)) {
            return NO_OWNER;
        } else if (testString.startsWith(UNCERTAIN)) {
            return CorrelationSituationType.UNCERTAIN;
        } else {
            return EXISTING_OWNER;
        }
    }

    String getExpectedOwnerName() {
        if (getExpectedCorrelationSituation() == EXISTING_OWNER) {
            return StringUtils.substringBefore(getTestString(), CANDIDATES_START);
        } else {
            return null;
        }
    }

    @Nullable Set<CandidateOwner> getCandidateOwners() {
        String testString = getTestString();
        int i = testString.indexOf(CANDIDATES_START);
        if (i < 0) {
            return null;
        }
        Set<CandidateOwner> candidateOwnerSet = new HashSet<>();
        String candidatesString = testString.substring(i + 1);
        String[] candidatesAndConfidences = candidatesString.split(CANDIDATES_SEPARATOR);
        for (String candidateAndConfidence : candidatesAndConfidences) {
            String[] parts = candidateAndConfidence.split(CANDIDATE_CONFIDENCE_SEPARATOR);
            CandidateOwner candidateOwner;
            if (parts.length == 1) {
                candidateOwner = new CandidateOwner(parts[0], null);
            } else if (parts.length == 2) {
                candidateOwner = new CandidateOwner(parts[0], Double.parseDouble(parts[1]));
            } else {
                throw new IllegalStateException("Wrong candidate-confidence pair: '" + candidateAndConfidence + "' in " + this);
            }
            candidateOwnerSet.add(candidateOwner);
        }
        return candidateOwnerSet;
    }
}
