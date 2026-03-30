/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

abstract class ConcurrencyRaceSelection extends ConcurrencyRaceDataFactory {

    protected List<WorkItem> collectRoundWorkItems(StressConfig config, int round) {
        return config.concurrencyMode() == ConcurrencyMode.DISTINCT_USERS
                ? collectDistinctUserRoundWorkItems(config, round)
                : collectSameUserConcurrentRoundWorkItems(config, round);
    }

    protected List<WorkItem> collectDistinctUserRoundWorkItems(StressConfig config, int round) {
        List<WorkItem> selected = selectedUsers(config);
        int hotSetSize = Math.min(config.hotSetSize(), selected.size());
        List<WorkItem> hot = new ArrayList<>(selected.subList(0, hotSetSize));
        List<WorkItem> remaining = new ArrayList<>(selected.subList(hotSetSize, selected.size()));

        Collections.shuffle(hot, new Random(41_000L + round));
        Collections.shuffle(remaining, new Random(43_000L + round));

        List<WorkItem> ordered = new ArrayList<>(selected.size());
        ordered.addAll(hot);
        ordered.addAll(remaining);
        return ordered;
    }

    protected List<WorkItem> collectSameUserConcurrentRoundWorkItems(StressConfig config, int round) {
        List<WorkItem> selected = selectedUsers(config);
        List<WorkItem> roundItems = new ArrayList<>(selected);
        int hotSetSize = Math.min(config.hotSetSize(), selected.size());
        List<WorkItem> hot = new ArrayList<>(selected.subList(0, hotSetSize));
        Collections.shuffle(hot, new Random(47_000L + round));
        for (int i = 0; i < config.hotRepeats(); i++) {
            roundItems.addAll(hot);
        }
        return roundItems;
    }

    protected void assertNoDuplicateUserOids(List<WorkItem> roundItems, StressConfig config, int round) {
        Map<String, Long> counts = roundItems.stream()
                .collect(Collectors.groupingBy(WorkItem::userOid, Collectors.counting()));
        List<String> duplicates = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            fail("Distinct-user round contains duplicate user OIDs. config=" + config + ", round=" + round + ", duplicates=" + duplicates);
        }
    }

    protected List<WorkItem> selectedUsers(StressConfig config) {
        int requestedUsers = config.users();
        return switch (config.cohortSelection()) {
            case MQL_ONLY -> new ArrayList<>(usersFor(new CohortKey(
                    TemplateType.MQL, config.scenarioSet(), config.mappingMode())).subList(0, requestedUsers));
            case XML_ONLY -> new ArrayList<>(usersFor(new CohortKey(
                    TemplateType.XML, config.scenarioSet(), config.mappingMode())).subList(0, requestedUsers));
            case MIXED -> {
                List<WorkItem> workItems = new ArrayList<>(usersFor(new CohortKey(
                        TemplateType.MQL, config.scenarioSet(), config.mappingMode())).subList(0, requestedUsers));
                workItems.addAll(usersFor(new CohortKey(
                        TemplateType.XML, config.scenarioSet(), config.mappingMode())).subList(0, requestedUsers));
                yield workItems;
            }
        };
    }

    protected List<AssignmentKey> expectedAssignments(WorkItem workItem) {
        int index = workItem.index();
        String prefix = targetSeedPrefix(workItem.cohortKey());
        return switch (workItem.cohortKey().scenarioSet()) {
            case ALL -> List.of(
                    new AssignmentKey(oid(prefix + "-service-" + index), SchemaConstants.ORG_OWNER.getLocalPart()),
                    new AssignmentKey(oid(prefix + "-org-workplace-" + index), SchemaConstants.ORG_DEFAULT.getLocalPart()),
                    new AssignmentKey(oid(prefix + "-org-manager-" + index), SchemaConstants.ORG_MANAGER.getLocalPart()),
                    new AssignmentKey(oid(prefix + "-role-expected-" + index), SchemaConstants.ORG_DEFAULT.getLocalPart()));
            case SERVICE_ONLY -> List.of(
                    new AssignmentKey(oid(prefix + "-service-" + index), SchemaConstants.ORG_OWNER.getLocalPart()));
            case ORG_BY_WORKPLACE_ONLY -> List.of(
                    new AssignmentKey(oid(prefix + "-org-workplace-" + index), SchemaConstants.ORG_DEFAULT.getLocalPart()));
            case ORG_BY_MANAGER_ONLY -> List.of(
                    new AssignmentKey(oid(prefix + "-org-manager-" + index), SchemaConstants.ORG_MANAGER.getLocalPart()));
            case ROLE_ONLY -> List.of(
                    new AssignmentKey(oid(prefix + "-role-expected-" + index), SchemaConstants.ORG_DEFAULT.getLocalPart()));
        };
    }

    protected List<WorkItem> usersFor(CohortKey cohortKey) {
        List<WorkItem> workItems = usersByCohort.get(cohortKey);
        if (workItems == null) {
            throw new IllegalStateException("Missing cohort users for " + cohortKey);
        }
        return workItems;
    }

    protected static List<CohortKey> activeCohorts() {
        List<CohortKey> cohorts = new ArrayList<>();
        for (TemplateType templateType : TemplateType.values()) {
            cohorts.add(new CohortKey(templateType, ScenarioSet.ALL, MappingMode.SINGLE));
            cohorts.add(new CohortKey(templateType, ScenarioSet.ALL, MappingMode.SHADOW));
            cohorts.add(new CohortKey(templateType, ScenarioSet.SERVICE_ONLY, MappingMode.SINGLE));
            cohorts.add(new CohortKey(templateType, ScenarioSet.ORG_BY_WORKPLACE_ONLY, MappingMode.SINGLE));
            cohorts.add(new CohortKey(templateType, ScenarioSet.ORG_BY_MANAGER_ONLY, MappingMode.SINGLE));
            cohorts.add(new CohortKey(templateType, ScenarioSet.ROLE_ONLY, MappingMode.SINGLE));
        }
        return cohorts;
    }
}
