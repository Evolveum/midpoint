/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util.cases;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Companion light-weight utilities for {@link CaseType} and {@link CaseWorkItemType}.
 */
public class CaseTypeUtil {

    @NotNull
    public static CaseType getCaseRequired(CaseWorkItemType workItem) {
        CaseType aCase = getCase(workItem);
        if (aCase == null) {
            throw new IllegalStateException("No case for work item " + workItem);
        }
        return aCase;
    }

    public static CaseType getCase(CaseWorkItemType workItem) {
        if (workItem == null) {
            return null;
        }

        @SuppressWarnings({ "unchecked", "raw" })
        PrismContainerable<CaseWorkItemType> parent = workItem.asPrismContainerValue().getParent();
        if (!(parent instanceof PrismContainer)) {
            return null;
        }
        PrismValue parentParent = ((PrismContainer<CaseWorkItemType>) parent).getParent();
        if (!(parentParent instanceof PrismObjectValue)) {
            return null;
        }
        @SuppressWarnings({ "unchecked", "raw" })
        PrismObjectValue<CaseType> parentParentPov = (PrismObjectValue<CaseType>) parentParent;
        return parentParentPov.asObjectable();
    }

    public static boolean isClosed(CaseType aCase) {
        return aCase != null && SchemaConstants.CASE_STATE_CLOSED.equals(aCase.getState());
    }

    public static XMLGregorianCalendar getStartTimestamp(CaseType aCase) {
        return aCase != null && aCase.getMetadata() != null ? aCase.getMetadata().getCreateTimestamp() : null;
    }

    public static String getRequesterComment(CaseType aCase) {
        OperationBusinessContextType businessContext = ApprovalContextUtil.getBusinessContext(aCase);
        return businessContext != null ? businessContext.getComment() : null;
    }

    public static boolean isCorrelationCase(@Nullable CaseType aCase) {
        return aCase != null && ObjectTypeUtil.hasArchetypeRef(aCase, SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value());
    }

    public static boolean isManualProvisioningCase(@Nullable CaseType aCase) {
        return aCase != null && ObjectTypeUtil.hasArchetypeRef(aCase, SystemObjectsType.ARCHETYPE_MANUAL_CASE.value());
    }

    public static boolean isApprovalCase(@Nullable CaseType aCase) {
        return aCase != null && ObjectTypeUtil.hasArchetypeRef(aCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    public static List<ObjectReferenceType> getAllCurrentAssignees(CaseType aCase) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (workItem.getCloseTimestamp() == null) {
                rv.addAll(workItem.getAssigneeRef());
            }
        }
        return rv;
    }

    public static boolean approvalSchemaExists(CaseType aCase) {
        return aCase != null
                && aCase.getApprovalContext() != null
                && aCase.getApprovalContext().getApprovalSchema() != null
                && !aCase.getApprovalContext().getApprovalSchema().asPrismContainerValue().isEmpty();
    }

    public static WorkItemId getId(CaseWorkItemType workItem) {
        return WorkItemId.of(workItem);
    }

    public static CaseWorkItemType getWorkItem(CaseType aCase, long id) {
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (workItem.getId() != null && workItem.getId() == id) {
                return workItem;
            }
        }
        return null;
    }

    public static boolean isCaseWorkItemNotClosed(CaseWorkItemType workItem) {
        return workItem != null && workItem.getCloseTimestamp() == null;
    }

    public static boolean isCaseWorkItemClosed(CaseWorkItemType workItem) {
        return workItem != null && workItem.getCloseTimestamp() != null;
    }

    public static boolean isWorkItemClaimable(CaseWorkItemType workItem) {
        return workItem != null
                && (workItem.getOriginalAssigneeRef() == null || StringUtils.isEmpty(workItem.getOriginalAssigneeRef().getOid()))
                && !doesAssigneeExist(workItem) && CollectionUtils.isNotEmpty(workItem.getCandidateRef());
    }

    public static boolean doesAssigneeExist(CaseWorkItemType workItem) {
        if (workItem == null || CollectionUtils.isEmpty(workItem.getAssigneeRef())) {
            return false;
        }
        for (ObjectReferenceType assignee : workItem.getAssigneeRef()) {
            if (StringUtils.isNotEmpty(assignee.getOid())) {
                return true;
            }
        }
        return false;
    }

    public static List<CaseWorkItemType> getWorkItemsForStage(CaseType aCase, int stageNumber) {
        return aCase.getWorkItem().stream()
                .filter(wi -> Objects.equals(wi.getStageNumber(), stageNumber))
                .collect(Collectors.toList());
    }
}
