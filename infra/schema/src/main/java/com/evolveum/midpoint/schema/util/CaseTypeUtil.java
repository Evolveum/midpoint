/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class CaseTypeUtil {

    @NotNull
    public static CaseType getCaseChecked(CaseWorkItemType workItem) {
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

        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerable<CaseWorkItemType> parent = workItem.asPrismContainerValue().getParent();
        if (!(parent instanceof PrismContainer)) {
            return null;
        }
        PrismValue parentParent = ((PrismContainer<CaseWorkItemType>) parent).getParent();
        if (!(parentParent instanceof PrismObjectValue)) {
            return null;
        }
        @SuppressWarnings({"unchecked", "raw"})
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

    public static boolean isManualProvisioningCase(CaseType aCase){
        if (aCase == null || CollectionUtils.isEmpty(aCase.getArchetypeRef())){
            return false;
        }
        return aCase != null && ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_MANUAL_CASE.value());
    }

    public static boolean isApprovalCase(CaseType aCase) {
        return aCase != null && ObjectTypeUtil.hasArchetype(aCase, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
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

    public static boolean approvalSchemaExists(CaseType aCase){
        return aCase != null && aCase.getApprovalContext() != null && aCase.getApprovalContext().getApprovalSchema() != null
                && !aCase.getApprovalContext().getApprovalSchema().asPrismContainerValue().isEmpty();
    }
}
