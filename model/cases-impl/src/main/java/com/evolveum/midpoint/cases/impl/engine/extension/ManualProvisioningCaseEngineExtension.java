/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCaseSchemaType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.impl.engine.CaseBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import static java.util.Objects.requireNonNull;

@Component
public class ManualProvisioningCaseEngineExtension extends DefaultEngineExtension {

    @Autowired
    public ManualProvisioningCaseEngineExtension(CaseBeans beans) {
        super(beans);
    }

    @Override
    public @NotNull Collection<String> getArchetypeOids() {
        return List.of(SystemObjectsType.ARCHETYPE_MANUAL_CASE.value());
    }

    @Override
    protected SimpleCaseSchemaType getCaseSchema(@NotNull CaseEngineOperation operation) {
        return operation.getCurrentCase().getManualProvisioningContext() != null ?
                operation.getCurrentCase().getManualProvisioningContext().getSchema() : null;
    }

    protected String selectOutcomeUri(List<String> outcomesFromEarliest, Set<String> uniqueOutcomes) {
        if (uniqueOutcomes.isEmpty()) {
            return OperationResultStatusType.SUCCESS.toString();
        } else if (uniqueOutcomes.size() == 1) {
            return requireNonNull(uniqueOutcomes.iterator().next());
        } else {
            return OperationResultStatusType.UNKNOWN.toString();
        }
    }
}
