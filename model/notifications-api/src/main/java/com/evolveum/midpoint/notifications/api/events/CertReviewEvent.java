/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
