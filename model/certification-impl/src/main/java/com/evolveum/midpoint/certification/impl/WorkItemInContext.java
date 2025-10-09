/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import org.jetbrains.annotations.NotNull;

record WorkItemInContext(
        @NotNull AccessCertificationCampaignType campaign,
        @NotNull AccessCertificationCaseType aCase,
        @NotNull AccessCertificationWorkItemType workItem) {
}
