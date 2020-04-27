/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ApprovalSchemaExecutionInformationUtil {
    public static ApprovalStageExecutionInformationType getStage(ApprovalSchemaExecutionInformationType executionInfo, int number) {
        return executionInfo.getStage().stream()
                .filter(i -> Objects.equals(number, i.getNumber()))
                .findAny()
                .orElse(null);
    }

    @NotNull
    public static PrismObject<CaseType> getEmbeddedCase(ApprovalSchemaExecutionInformationType executionInfo) {
        if (executionInfo.getCaseRef() == null) {
            throw new IllegalStateException("No caseRef in " + executionInfo);
        } else if (executionInfo.getCaseRef().getObject() == null) {
            throw new IllegalStateException("No caseRef.object in " + executionInfo);
        } else {
            //noinspection unchecked
            return executionInfo.getCaseRef().getObject();
        }
    }

    @NotNull
    public static CaseType getEmbeddedCaseBean(ApprovalSchemaExecutionInformationType executionInfo) {
        return getEmbeddedCase(executionInfo).asObjectable();
    }
}
