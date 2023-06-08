/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
