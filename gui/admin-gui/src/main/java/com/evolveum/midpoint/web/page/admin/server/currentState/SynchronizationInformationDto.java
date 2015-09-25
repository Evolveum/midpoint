/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

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

    public SynchronizationInformationDto(SynchronizationInformationType synchronizationInformationType) {
        this.synchronizationInformationType = synchronizationInformationType;
    }

    public int getCountProtected() {
        return synchronizationInformationType.getCountProtected();
    }

    public int getCountNoSynchronizationPolicy() {
        return synchronizationInformationType.getCountNoSynchronizationPolicy();
    }

    public int getCountSynchronizationDisabled() {
        return synchronizationInformationType.getCountSynchronizationDisabled();
    }

    public int getCountNotApplicableForTask() {
        return synchronizationInformationType.getCountNotApplicableForTask();
    }

    public int getCountDeleted() {
        return synchronizationInformationType.getCountDeleted();
    }

    public int getCountDisputed() {
        return synchronizationInformationType.getCountDisputed();
    }

    public int getCountLinked() {
        return synchronizationInformationType.getCountLinked();
    }

    public int getCountUnlinked() {
        return synchronizationInformationType.getCountUnlinked();
    }

    public int getCountUnmatched() {
        return synchronizationInformationType.getCountUnmatched();
    }
}
