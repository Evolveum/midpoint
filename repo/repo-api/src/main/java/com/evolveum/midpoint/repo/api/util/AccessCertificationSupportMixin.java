/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.util;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AccessCertificationCaseId;
import com.evolveum.midpoint.schema.util.AccessCertificationWorkItemId;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

/**
 * Methods that facilitate working with {@link AccessCertificationCampaignType} and related objects at the repository level.
 * To be seen if really useful.
 */
@Experimental
public interface AccessCertificationSupportMixin {

    default @NotNull AccessCertificationCaseType getAccessCertificationCase(
            @NotNull AccessCertificationCaseId id,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        var repo = repositoryService();
        var cases = repo.searchContainers(AccessCertificationCaseType.class, id.queryFor(), options, result);
        var aCase = MiscUtil.extractSingleton(
                cases,
                () -> new IllegalStateException("Multiple cases matching '" + id + "'? " + cases));
        if (aCase != null) {
            return aCase;
        } else {
            throw new ObjectNotFoundException(
                    "Certification case " + id + " could not be found",
                    AccessCertificationCaseType.class, null);
        }
    }

    default @NotNull AccessCertificationWorkItemType getAccessCertificationWorkItem(
            @NotNull AccessCertificationWorkItemId id,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        var aCase = getAccessCertificationCase(id.caseId(), options, result);
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(aCase, id.workItemId());
        if (workItem == null) {
            throw new ObjectNotFoundException(
                    "Work item " + id + " could not be found in " + aCase,
                    AccessCertificationCaseType.class, null);
        } else {
            return workItem;
        }
    }

    @NotNull RepositoryService repositoryService();
}
