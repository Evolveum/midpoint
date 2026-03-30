/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;

abstract class ConcurrencyRaceEnums extends AbstractEmptyModelIntegrationTest {

    protected enum TemplateType {
        MQL("MQL"),
        XML("XML");

        final String prefix;

        TemplateType(String prefix) {
            this.prefix = prefix;
        }
    }

    protected enum CohortSelection {
        MQL_ONLY,
        XML_ONLY,
        MIXED
    }

    protected enum MappingMode {
        SINGLE("single", "S"),
        SHADOW("shadow", "H");

        final String suffix;
        final String userTag;

        MappingMode(String suffix, String userTag) {
            this.suffix = suffix;
            this.userTag = userTag;
        }
    }

    protected enum ScenarioSet {
        ALL("all", "ALL", "ALL"),
        SERVICE_ONLY("service-only", "SERVICE", "SERVICE"),
        ORG_BY_WORKPLACE_ONLY("org-workplace-only", "ORGWP", "ORGWP"),
        ORG_BY_MANAGER_ONLY("org-manager-only", "ORGMGR", "ORGMGR"),
        ROLE_ONLY("role-only", "ROLE", "ROLE");

        final String suffix;
        final String userTag;
        final String targetTag;

        ScenarioSet(String suffix, String userTag, String targetTag) {
            this.suffix = suffix;
            this.userTag = userTag;
            this.targetTag = targetTag;
        }
    }

    protected enum ConcurrencyMode {
        DISTINCT_USERS,
        SAME_USER_CONCURRENT
    }

    protected enum ValidationPhase {
        IMMEDIATE,
        POST_ROUND
    }

    protected record Failure(
            String configName,
            CohortSelection cohortSelection,
            MappingMode mappingMode,
            ScenarioSet scenarioSet,
            ConcurrencyMode concurrencyMode,
            ValidationPhase validationPhase,
            TemplateType templateType,
            String userOid,
            int userIndex,
            int round,
            int threadIndex,
            String stackTrace) {

        static Failure of(
                ConcurrencyRaceConfigs.StressConfig config,
                WorkItem workItem,
                int round,
                int threadIndex,
                ValidationPhase validationPhase,
                Throwable throwable) {
            return new Failure(
                    config.name(),
                    config.cohortSelection(),
                    config.mappingMode(),
                    config.scenarioSet(),
                    config.concurrencyMode(),
                    validationPhase,
                    workItem != null ? workItem.templateType() : null,
                    workItem != null ? workItem.userOid() : null,
                    workItem != null ? workItem.index() : -1,
                    round,
                    threadIndex,
                    ConcurrencyRaceEnums.stackTrace(throwable));
        }

        @NotNull String format() {
            return """
                    config=%s
                    cohortSelection=%s
                    mappingMode=%s
                    scenarioSet=%s
                    concurrencyMode=%s
                    validationPhase=%s
                    templateMode=%s
                    userIndex=%s
                    userOid=%s
                    round=%s
                    threadIndex=%s
                    %s
                    """.formatted(
                    configName,
                    cohortSelection,
                    mappingMode,
                    scenarioSet,
                    concurrencyMode,
                    validationPhase,
                    templateType,
                    userIndex,
                    userOid,
                    round,
                    threadIndex,
                    stackTrace);
        }
    }

    protected record AssignmentKey(String oid, String relation) {
    }

    protected record CohortKey(TemplateType templateType, ScenarioSet scenarioSet, MappingMode mappingMode) {
    }

    protected record WorkItem(int index, CohortKey cohortKey, String userOid, UserType user) {
        TemplateType templateType() {
            return cohortKey.templateType();
        }
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
