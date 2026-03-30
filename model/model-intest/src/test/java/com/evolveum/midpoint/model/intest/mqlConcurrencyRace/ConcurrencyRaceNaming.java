/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

abstract class ConcurrencyRaceNaming extends ConcurrencyRaceState {

    protected static String subtypeFor(CohortKey cohortKey) {
        return "mql-concurrency-race-"
                + cohortKey.templateType().prefix.toLowerCase()
                + "-"
                + cohortKey.scenarioSet().suffix
                + "-"
                + cohortKey.mappingMode().suffix;
    }

    protected static String userPrefix(CohortKey cohortKey) {
        return cohortKey.templateType().prefix + "-" + cohortKey.scenarioSet().userTag + "-" + cohortKey.mappingMode().userTag;
    }

    protected static String targetSeedPrefix(CohortKey cohortKey) {
        return cohortKey.templateType().prefix + "-" + cohortKey.scenarioSet().targetTag + "-" + cohortKey.mappingMode().suffix;
    }

    protected static String workplaceId(int index, CohortKey cohortKey) {
        return targetSeedPrefix(cohortKey) + "-PM-" + format(index);
    }

    protected static String personalNumber(int index, CohortKey cohortKey) {
        return targetSeedPrefix(cohortKey) + "-PN-" + format(index);
    }

    protected static String sapCode1(int index, CohortKey cohortKey) {
        return targetSeedPrefix(cohortKey) + "-SAP-" + format(index);
    }

    protected static String format(int index) {
        return String.format("%05d", index);
    }

    protected static String oid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
