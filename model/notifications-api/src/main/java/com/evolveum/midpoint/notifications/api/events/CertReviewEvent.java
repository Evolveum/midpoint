/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Event for certification reviewers (reminding him/her of the need to do the work).
 */
public interface CertReviewEvent extends AccessCertificationEvent {

    /**
     * Actual reviewer - the person which the work item is assigned to.
     * This is never his/her deputy.
     */
    SimpleObjectRef getActualReviewer();

    /**
     * List of cases that await response from the actual reviewer.
     */
    @NotNull
    Collection<AccessCertificationCaseType> getCasesAwaitingResponseFromActualReviewer();

    List<AccessCertificationCaseType> getCases();
}
