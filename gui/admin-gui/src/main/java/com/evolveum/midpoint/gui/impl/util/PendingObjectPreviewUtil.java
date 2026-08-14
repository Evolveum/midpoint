/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseState;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Resolves the approval state of a pending object preview from its source case.
 *
 * For root operation-request cases, matching child approval cases are inspected.
 */
public final class PendingObjectPreviewUtil {

    /**
     * Approval classification of a transient pending-object preview.
     */
    public enum ApprovalState {
        AWAITING_APPROVAL,
        REJECTED,
        UNKNOWN
    }

    private PendingObjectPreviewUtil() {
    }

    public static <O extends ObjectType> ApprovalState determineApprovalState(
            CaseType sourceCase, Class<O> expectedType, String expectedOid, PageBase page, OperationResult result) {
        if (WebComponentUtil.getObjectFromAddDeltaForCase(sourceCase) != null) {
            return determineApprovalCaseState(sourceCase);
        }

        if (sourceCase == null
                || StringUtils.isBlank(sourceCase.getOid())
                || !ObjectTypeUtil.hasArchetypeRef(sourceCase, SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())) {
            return ApprovalState.UNKNOWN;
        }

        var query = page.getPrismContext().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF)
                .ref(sourceCase.getOid())
                .build();
        List<PrismObject<CaseType>> childCases = WebModelServiceUtils.searchObjects(CaseType.class, query, result, page);
        for (PrismObject<CaseType> childCase : childCases) {
            CaseType child = childCase.asObjectable();
            if (WebComponentUtil.getObjectFromAddDeltaForCase(child) == null) {
                continue;
            }
            PrismObject<O> childObject = WebComponentUtil.getPendingObjectFromAddCase(child, expectedType, expectedOid);
            if (childObject != null) {
                return determineApprovalCaseState(child);
            }
        }

        return ApprovalState.UNKNOWN;
    }

    private static ApprovalState determineApprovalCaseState(CaseType aCase) {
        if (!CaseTypeUtil.isApprovalCase(aCase)) {
            return ApprovalState.UNKNOWN;
        }

        Boolean approved;
        try {
            approved = ApprovalUtils.approvalBooleanValue(aCase.getOutcome());
        } catch (IllegalArgumentException e) {
            return ApprovalState.UNKNOWN;
        }
        if (Boolean.FALSE.equals(approved)) {
            return ApprovalState.REJECTED;
        }
        if (approved == null && CaseState.of(aCase).isOpen()) {
            return ApprovalState.AWAITING_APPROVAL;
        }
        return ApprovalState.UNKNOWN;
    }
}
