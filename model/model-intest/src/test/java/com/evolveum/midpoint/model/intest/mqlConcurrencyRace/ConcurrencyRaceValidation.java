/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

abstract class ConcurrencyRaceValidation extends ConcurrencyRaceSelection {

    protected void validateUser(
            StressConfig config,
            WorkItem workItem,
            int round,
            int threadIndex,
            ValidationPhase validationPhase) throws CommonException {
        UserType user = getUser(workItem.userOid()).asObjectable();

        List<AssignmentKey> actualAssignments = user.getAssignment().stream()
                .filter(assignment -> assignment.getTargetRef() != null && assignment.getTargetRef().getOid() != null)
                .map(ConcurrencyRaceValidation::toAssignmentKey)
                .sorted(Comparator.comparing(AssignmentKey::oid).thenComparing(AssignmentKey::relation))
                .toList();
        List<AssignmentKey> expectedAssignments = expectedAssignments(workItem).stream()
                .sorted(Comparator.comparing(AssignmentKey::oid).thenComparing(AssignmentKey::relation))
                .toList();

        if (!actualAssignments.equals(expectedAssignments)) {
            throw new IllegalStateException(diagnosticForMismatch(
                    config, workItem, round, threadIndex, validationPhase, user, actualAssignments, expectedAssignments));
        }
    }

    protected String diagnosticForMismatch(
            StressConfig config,
            WorkItem workItem,
            int round,
            int threadIndex,
            ValidationPhase validationPhase,
            UserType user,
            List<AssignmentKey> actualAssignments,
            List<AssignmentKey> expectedAssignments) {
        return """
                %s
                mode=%s
                templateMode=%s
                mappingMode=%s
                scenarioSet=%s
                validationPhase=%s
                round=%s
                thread=%s
                userOid=%s
                userName=%s
                subtype=%s
                personalNumber=%s
                workplaceId=%s
                sapCode1=%s
                expectedAssignments=%s
                actualAssignments=%s
                rawMql.service=%s
                rawMql.serviceShadow=%s
                rawMql.orgByWorkplace=%s
                rawMql.orgByWorkplaceShadow=%s
                rawMql.orgByManager=%s
                rawMql.orgByManagerShadow=%s
                rawMql.role=%s
                rawMql.roleShadow=%s
                """.formatted(
                mismatchHeadline(config, actualAssignments, expectedAssignments),
                config.concurrencyMode(),
                workItem.templateType(),
                config.mappingMode(),
                config.scenarioSet(),
                validationPhase,
                round,
                threadIndex,
                workItem.userOid(),
                user.getName() != null ? user.getName().getOrig() : null,
                user.getSubtype(),
                user.getPersonalNumber(),
                readExtensionValue(user, RACE_WORKPLACE_ID),
                readExtensionValue(user, RACE_SAP_CODE_1),
                expectedAssignments,
                actualAssignments,
                RAW_MQL_A.strip(),
                RAW_MQL_A_SHADOW.strip(),
                RAW_MQL_B1,
                RAW_MQL_B1_SHADOW,
                RAW_MQL_B2,
                RAW_MQL_B2_SHADOW,
                RAW_MQL_C.strip(),
                RAW_MQL_C_SHADOW.strip());
    }

    protected String mismatchHeadline(
            StressConfig config,
            List<AssignmentKey> actualAssignments,
            List<AssignmentKey> expectedAssignments) {
        Map<AssignmentKey, Long> actualCounts = actualAssignments.stream()
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()));
        Map<AssignmentKey, Long> expectedCounts = expectedAssignments.stream()
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()));

        List<AssignmentKey> duplicated = actualCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > expectedCounts.getOrDefault(entry.getKey(), 0L))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(AssignmentKey::oid).thenComparing(AssignmentKey::relation))
                .toList();

        if (!duplicated.isEmpty()) {
            return """
                    Duplicate assignment bug reproduced under concurrency.
                    concurrencyMode=%s
                    cohortSelection=%s
                    mappingMode=%s
                    scenarioSet=%s
                    duplicatedAssignments=%s
                    """.formatted(config.concurrencyMode(), config.cohortSelection(), config.mappingMode(), config.scenarioSet(), duplicated);
        }
        return "Assignment mismatch under concurrency.";
    }

    protected String renderFailures(StressConfig config, Queue<Failure> failures) {
        String details = failures.stream()
                .limit(5)
                .map(Failure::format)
                .collect(Collectors.joining("\n\n"));
        return """
                MQL concurrency race test failed.
                config=%s
                failureCount=%s

                %s
                """.formatted(config, failures.size(), details);
    }

    protected static AssignmentKey toAssignmentKey(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        String relation = targetRef.getRelation() != null
                ? targetRef.getRelation().getLocalPart()
                : SchemaConstants.ORG_DEFAULT.getLocalPart();
        return new AssignmentKey(targetRef.getOid(), relation);
    }

    protected static Object readExtensionValue(FocusType focus, QName itemName) {
        if (focus.getExtension() == null) {
            return null;
        }
        PrismProperty<?> property = (PrismProperty<?>) focus.getExtension().asPrismContainerValue()
                .findItem(ItemPath.create(itemName), PrismProperty.class);
        return property != null ? property.getAnyRealValue() : null;
    }
}
