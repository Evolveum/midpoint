/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author Pavol Mederly
 */
public class SynchronizationInformationDto {

    public static final String F_COUNT_PROTECTED = "countProtected";
    public static final String F_COUNT_NO_SYNCHRONIZATION_POLICY = "countNoSynchronizationPolicy";
    public static final String F_COUNT_SYNCHRONIZATION_DISABLED = "countSynchronizationDisabled";
    public static final String F_COUNT_NOT_APPLICABLE_FOR_TASK = "countNotApplicableForTask";
    public static final String F_COUNT_DELETED = "countDeleted";
    public static final String F_COUNT_DISPUTED = "countDisputed";
    public static final String F_COUNT_LINKED = "countLinked";
    public static final String F_COUNT_UNLINKED = "countUnlinked";
    public static final String F_COUNT_UNMATCHED = "countUnmatched";

    private SynchronizationInformationType synchronizationInformationType;
	private boolean useAfter;

    public SynchronizationInformationDto(SynchronizationInformationType synchronizationInformationType, boolean useAfter) {
        this.synchronizationInformationType = synchronizationInformationType;
		this.useAfter = useAfter;
    }

    public int getCountProtected() {
        return useAfter ? synchronizationInformationType.getCountProtectedAfter() : synchronizationInformationType.getCountProtected();
    }

    public int getCountNoSynchronizationPolicy() {
        return useAfter ? synchronizationInformationType.getCountNoSynchronizationPolicyAfter() : synchronizationInformationType.getCountNoSynchronizationPolicy();
    }

    public int getCountSynchronizationDisabled() {
        return useAfter ? synchronizationInformationType.getCountSynchronizationDisabledAfter() : synchronizationInformationType.getCountSynchronizationDisabled();
    }

    public int getCountNotApplicableForTask() {
        return useAfter ? synchronizationInformationType.getCountNotApplicableForTaskAfter() : synchronizationInformationType.getCountNotApplicableForTask();
    }

    public int getCountDeleted() {
        return useAfter ? synchronizationInformationType.getCountDeletedAfter() : synchronizationInformationType.getCountDeleted();
    }

    public int getCountDisputed() {
        return useAfter ? synchronizationInformationType.getCountDisputedAfter() : synchronizationInformationType.getCountDisputed();
    }

    public int getCountLinked() {
        return useAfter ? synchronizationInformationType.getCountLinkedAfter() : synchronizationInformationType.getCountLinked();
    }

    public int getCountUnlinked() {
        return useAfter ? synchronizationInformationType.getCountUnlinkedAfter() : synchronizationInformationType.getCountUnlinked();
    }

    public int getCountUnmatched() {
        return useAfter ? synchronizationInformationType.getCountUnmatchedAfter() : synchronizationInformationType.getCountUnmatched();
    }
}
