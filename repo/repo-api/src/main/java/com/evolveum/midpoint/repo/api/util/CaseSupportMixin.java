/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.util;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Methods that facilitate working with {@link CaseType} and {@link CaseWorkItemType} objects at the repository level.
 * To be seen if really useful.
 */
@Experimental
public interface CaseSupportMixin {

    default @NotNull CaseWorkItemType getWorkItem(
            @NotNull WorkItemId id,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        var repo = repositoryService();
        var aCase = repo.getObject(CaseType.class, id.caseOid, options, result);
        Object workItem = aCase.find(id.asItemPath());
        if (workItem == null) {
            throw new ObjectNotFoundException(
                    "Work item " + id + " could not be found in " + aCase,
                    CaseWorkItemType.class,
                    null);
        } else {
            //noinspection unchecked
            return ((PrismContainerValue<CaseWorkItemType>) workItem).asContainerable();
        }
    }

    @NotNull RepositoryService repositoryService();
}
