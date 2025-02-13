/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.schema.statistics.ComponentsPerformanceComputer.ComponentDescription;
import com.evolveum.midpoint.schema.statistics.ComponentsPerformanceComputer.RegexBasedComponentDescription;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * The basic structure of components for performance statistics purposes. It goes mostly by midPoint top level packages,
 * not distinguishing e.g. between parts of the model (clockwork, projector, and its constituents).
 *
 * Note that the order of components is important, as the first match counts.
 */
enum BasicComponentStructure {

    MODEL("Model",
            "com.evolveum.midpoint.model.*"),

    NOTIFICATIONS("Notifications",
            "com.evolveum.midpoint.notifications.*"),

    APPROVALS("Approvals",
            "com.evolveum.midpoint.wf.*"),

    PROVISIONING("Provisioning",
            "com.evolveum.midpoint.provisioning.api.*",
            "com.evolveum.midpoint.provisioning.impl.*",
            "com.evolveum.midpoint.provisioning.util.*"),

    UCF("UCF",
            "com.evolveum.midpoint.provisioning.ucf.*"),

    CONN_ID("ConnId",
            "org.identityconnectors.framework.*"),

    TASKS("Tasks",
            "com.evolveum.midpoint.task.*"),

    REPOSITORY_COMMON("Repository - common",
            "com.evolveum.midpoint.repo.common.*"),

    REPOSITORY_CACHE("Repository cache",
            "com.evolveum.midpoint.repo.cache.*"),

    REPOSITORY("Repository",
            "com.evolveum.midpoint.repo.*",
            "SqaleRepositoryService.*"),

    AUDIT("Audit",
            "com.evolveum.midpoint.audit.*", "SqaleAuditService.*");

    // TODO what about these?
    // com.evolveum.midpoint.gui
    // com.evolveum.midpoint.init
    // com.evolveum.midpoint.web
    // com.evolveum.midpoint.authentication
    // com.evolveum.midpoint.cases
    // com.evolveum.midpoint.casemgmt
    // com.evolveum.midpoint.certification
    // com.evolveum.midpoint.common
    // com.evolveum.midpoint.report
    // com.evolveum.midpoint.rest
    // com.evolveum.midpoint.schema
    // com.evolveum.midpoint.security
    // com.evolveum.midpoint.util

    private final ComponentDescription description;

    BasicComponentStructure(String componentName, String... includePatterns) {
        description = new RegexBasedComponentDescription(
                componentName,
                Arrays.stream(includePatterns)
                        .map(MiscUtil::toRegex)
                        .toList(),
                List.of());
    }

    public ComponentDescription getDescription() {
        return description;
    }

    static List<ComponentDescription> componentDescriptions() {
        return Arrays.stream(values())
                .map(BasicComponentStructure::getDescription)
                .toList();
    }
}
