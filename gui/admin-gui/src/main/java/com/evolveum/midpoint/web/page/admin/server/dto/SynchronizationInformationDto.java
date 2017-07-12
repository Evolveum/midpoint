/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
