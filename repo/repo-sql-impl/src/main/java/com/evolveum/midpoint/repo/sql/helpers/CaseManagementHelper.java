/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import java.util.Collection;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetContainerableIdOnlyResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Contains methods specific to handle case management work items.
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 */
@Component(value = "repoCaseManagementHelper")
public class CaseManagementHelper {

    @Autowired private ObjectRetriever objectRetriever;

    // TODO find a better name
    CaseWorkItemType updateLoadedCaseWorkItem(
            GetContainerableIdOnlyResult result,
            Map<String, PrismObject<CaseType>> ownersMap,
            EntityManager em,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException, DtoTranslationException {

        String ownerOid = result.getOwnerOid();
        Integer id = result.getId();
        PrismObject<CaseType> aCase = resolveCase(ownerOid, ownersMap, em);
        PrismContainer<Containerable> workItemContainer = aCase.findContainer(CaseType.F_WORK_ITEM);
        if (workItemContainer == null) {
            throw new ObjectNotFoundException(
                    "Case " + aCase + " has no work items even if it should have " + result,
                    CaseWorkItemType.class,
                    ownerOid + ":" + id,
                    GetOperationOptions.isAllowNotFound(options));
        }
        PrismContainerValue<?> workItemPcv = workItemContainer.findValue(id);
        if (workItemPcv == null) {
            throw new ObjectNotFoundException("Case " + aCase + " has no work item " + result,
                    CaseWorkItemType.class,
                    ownerOid + ":" + id,
                    GetOperationOptions.isAllowNotFound(options));
        }
        return (CaseWorkItemType) workItemPcv.asContainerable();
    }

    @NotNull
    private PrismObject<CaseType> resolveCase(
            String caseOid, Map<String, PrismObject<CaseType>> cache, EntityManager em)
            throws DtoTranslationException, ObjectNotFoundException, SchemaException {

        PrismObject<CaseType> aCase = cache.get(caseOid);
        if (aCase != null) {
            return aCase;
        }
        aCase = objectRetriever.getObjectInternal(em, CaseType.class, caseOid, null, false);
        cache.put(caseOid, aCase);
        return aCase;
    }

}
