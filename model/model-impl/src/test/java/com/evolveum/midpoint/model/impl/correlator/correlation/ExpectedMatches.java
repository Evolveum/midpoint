/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.Match;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;

class ExpectedMatches {

    @NotNull private final PathKeyedMap<Match> matches = new PathKeyedMap<>();

    public void add(String propertyAbbreviation, String matchAbbreviation) {
        matches.put(
                getPath(propertyAbbreviation),
                getMatch(matchAbbreviation));
    }

    private Match getMatch(String abbreviation) {
        switch (abbreviation.toUpperCase()) {
            case "F":
                return Match.FULL;
            case "P":
                return Match.PARTIAL;
            case "N":
                return Match.NONE;
            case "NA":
                return Match.NOT_APPLICABLE;
            default:
                throw new IllegalArgumentException(abbreviation);
        }
    }

    private ItemPath getPath(String abbreviation) {
        switch (abbreviation.toLowerCase()) {
            case "en":
                return UserType.F_EMPLOYEE_NUMBER;
            case "gn":
                return UserType.F_GIVEN_NAME;
            case "fn":
                return UserType.F_FAMILY_NAME;
            case "dob":
                return ItemPath.fromString("extension/dateOfBirth");
            case "id":
                return ItemPath.fromString("extension/nationalId");
            default:
                throw new IllegalArgumentException(abbreviation);
        }
    }

    public @NotNull PathKeyedMap<Match> getMatches() {
        return matches;
    }
}
