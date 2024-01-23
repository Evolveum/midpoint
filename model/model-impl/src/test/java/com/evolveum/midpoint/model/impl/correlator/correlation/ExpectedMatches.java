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
        return switch (abbreviation.toUpperCase()) {
            case "F" -> Match.FULL;
            case "P" -> Match.PARTIAL;
            case "N" -> Match.NONE;
            case "NA" -> Match.NOT_APPLICABLE;
            default -> throw new IllegalArgumentException(abbreviation);
        };
    }

    private ItemPath getPath(String abbreviation) {
        return switch (abbreviation.toLowerCase()) {
            case "en" -> UserType.F_EMPLOYEE_NUMBER;
            case "gn" -> UserType.F_GIVEN_NAME;
            case "fn" -> UserType.F_FAMILY_NAME;
            case "hp" -> UserType.F_HONORIFIC_PREFIX;
            case "cc" -> UserType.F_COST_CENTER;
            case "dob" -> ItemPath.fromString("extension/dateOfBirth");
            case "id" -> ItemPath.fromString("extension/nationalId");
            default -> throw new IllegalArgumentException(abbreviation);
        };
    }

    public @NotNull PathKeyedMap<Match> getMatches() {
        return matches;
    }
}
