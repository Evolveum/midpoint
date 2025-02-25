/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class WorkItemTypeUtil {

    public static AbstractWorkItemOutputType getOutput(AbstractWorkItemType workItem) {
        return workItem != null ? workItem.getOutput() : null;
    }

    public static String getOutcome(AbstractWorkItemType workItem) {
        return getOutcome(getOutput(workItem));
    }

    public static String getComment(AbstractWorkItemType workItem) {
        return getComment(getOutput(workItem));
    }

    public static String getComment(AbstractWorkItemOutputType output) {
        return output != null ? output.getComment() : null;
    }

    public static void setEvidence(AbstractWorkItemType workItem, byte[] file) {
        if (workItem == null) {
            return;
        }
        if (workItem.getOutput() == null) {
            workItem.setOutput(new AbstractWorkItemOutputType());
        }
        workItem.getOutput().setEvidence(file);
    }

    public static byte[] getEvidence(AbstractWorkItemType workItem) {
        return getEvidence(getOutput(workItem));
    }

    public static byte[] getEvidence(AbstractWorkItemOutputType output) {
        return output != null ? output.getEvidence() : null;
    }

    public static String getEvidenceFilename(AbstractWorkItemType workItem) {
        return getEvidenceFilename(getOutput(workItem));
    }

    public static String getEvidenceFilename(AbstractWorkItemOutputType output) {
        return output != null ? output.getEvidenceFilename() : null;
    }

    public static String getEvidenceContentType(AbstractWorkItemType workItem) {
        return getEvidenceContentType(getOutput(workItem));
    }

    public static String getEvidenceContentType(AbstractWorkItemOutputType output) {
        return output != null ? output.getEvidenceContentType() : null;
    }

    public static String getOutcome(AbstractWorkItemOutputType output) {
        return output != null ? output.getOutcome() : null;
    }

    public static void assertHasCaseOid(CaseWorkItemType workItem) {
        if (CaseTypeUtil.getCase(workItem) == null) {
            throw new AssertionError("No parent case for work item " + workItem);
        }
    }

    public static ObjectReferenceType getRequestorReference(CaseWorkItemType workItem){
        CaseType caseType = CaseTypeUtil.getCase(workItem);
        if (caseType != null){
            return caseType.getRequestorRef();
        }
        return null;
    }

    public static ObjectReferenceType getObjectReference(CaseWorkItemType workItem){
        CaseType caseType = CaseTypeUtil.getCase(workItem);
        if (caseType != null){
            return caseType.getObjectRef();
        }
        return null;
    }


    public static ObjectReferenceType getTargetReference(CaseWorkItemType workItem){
        CaseType caseType = CaseTypeUtil.getCase(workItem);
        if (caseType != null){
            return caseType.getTargetRef();
        }
        return null;
    }

    public static int getEscalationLevelNumber(AbstractWorkItemType workItem) {
        return getEscalationLevelNumber(workItem.getEscalationLevel());
    }

    public static int getEscalationLevelNumber(WorkItemEscalationLevelType level) {
        return level != null && level.getNumber() != null ? level.getNumber() : 0;
    }

    public static String getEscalationLevelName(WorkItemEscalationLevelType level) {
        return level != null ? level.getName() : null;
    }

    public static String getEscalationLevelDisplayName(WorkItemEscalationLevelType level) {
        return level != null ? level.getDisplayName() : null;
    }

    public static String getEscalationLevelName(AbstractWorkItemType workItem) {
        return getEscalationLevelName(workItem.getEscalationLevel());
    }

    public static String getEscalationLevelDisplayName(AbstractWorkItemType workItem) {
        return getEscalationLevelDisplayName(workItem.getEscalationLevel());
    }

    public static boolean isWithoutOutcome(AccessCertificationWorkItemType workItem) {
        return getOutcome(workItem) == null;
    }

    public static boolean isOpened(AccessCertificationWorkItemType workItem) {
        return workItem.getCloseTimestamp() == null;
    }

}
